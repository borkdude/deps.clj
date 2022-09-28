(ns borkdude.deps-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [process]]
   [borkdude.deps :as deps]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

(def invoke-deps-cmd
  "Returns the command to invoke `borkdude.deps` on the current system,
  based on the value of env variable `DEPS_CLJ_TEST_ENV`."
  (case (System/getenv "DEPS_CLJ_TEST_ENV")
    "babashka" (let [classpath (str/join deps/path-separator ["src" "test" "resources"])]
                 (str "bb -cp " classpath " -m borkdude.deps "))
    "native" "./deps "
    (cond->>
     "clojure -M -m borkdude.deps "
     deps/windows?
     (str "powershell -NoProfile -Command "))))

(deftest parse-args-test
  (is (= {:mode :repl, :jvm-opts ["-Dfoo=bar" "-Dbaz=quuz"]}
         (deps/parse-args ["-J-Dfoo=bar" "-J-Dbaz=quuz"])))
  (is (= {:mode :main, :main-aliases nil, :args '("-e" "(+ 1 2 3)")}
         (deps/parse-args ["-M" "-e" "(+ 1 2 3)"])))
  (is (= {:mode :main, :main-aliases ":foo", :args nil} (deps/parse-args ["-M:foo"]))))

(deftest path-test
  (is (str/includes? (with-out-str
                       (deps/-main "-Spath")) "resources")))

(deftest describe-test
  (let [{:keys [:config-files :config-user :config-project :deps-clj-version :version]}
        (edn/read-string (with-out-str
                           (deps/-main "-Sdescribe")))]
    (is (every? some? [config-files config-user config-project deps-clj-version version]))))

(deftest tree-test
  (binding [deps/*exit-fn* (constantly nil)]
    (is (str/includes?
         (with-out-str
           (deps/-main "-Stree"))
         "org.clojure/clojure"))))

(deftest exec-test
  (deps/-main "-X:exec-test" ":foo" "1")
  (is (= "{:foo 1}" (slurp "exec-fn-test")))
  (.delete (io/file "exec-fn-test"))
  (is (do (deps/-main "-X" "clojure.core/prn" ":foo" "1")
          ::success)))

(when (not deps/windows?)
  (deftest whitespace-test
    (testing "jvm opts"
      (let [temp-dir (fs/create-temp-dir)
            temp-file (fs/create-file (fs/path temp-dir "temp.txt"))
            temp-file-path (str temp-file)
            _ (deps/-main "-Sdeps" "{:aliases {:space {:jvm-opts [\"-Dfoo=\\\"foo bar\\\"\"]}}}" "-M:space" "-e"
                          (format "(spit \"%s\" (System/getProperty \"foo\"))"
                                  temp-file-path))
            out (slurp temp-file-path)]
        (is (= "\"foo bar\"" out))))
    (testing "main opts"
      (let [temp-dir (fs/create-temp-dir)
            temp-file (fs/create-file (fs/path temp-dir "temp.txt"))
            temp-file-path (str temp-file)
            _ (deps/-main "-Sdeps"
                          (format "{:aliases {:space {:main-opts [\"-e\" \"(spit \\\"%s\\\" (+ 1 2 3))\"]}}}"
                                  temp-file-path)
                          "-M:space")
            out (slurp temp-file-path)]
        (is (= "6" out))))))

(deftest jvm-proxy-settings-test
  (is (= {:host "aHost" :port "1234"} (deps/parse-proxy-info "http://aHost:1234")))
  (is (= {:host "aHost" :port "1234"} (deps/parse-proxy-info "http://user:pw@aHost:1234")))
  (is (= {:host "aHost" :port "1234"} (deps/parse-proxy-info "https://aHost:1234")))
  (is (= {:host "aHost" :port "1234"} (deps/parse-proxy-info "https://user:pw@aHost:1234")))
  (is (nil? (deps/parse-proxy-info "http://aHost:abc"))))

(when (not deps/windows?)
  (deftest jvm-opts-test
    (let [temp-dir (fs/create-temp-dir)
          temp-file (fs/create-file (fs/path temp-dir "temp.txt"))
          temp-file-path (str temp-file)]
      (deps/-main "-J-Dfoo=bar" "-J-Dbaz=quux"
                  "-M" "-e" (format "
(spit \"%s\" (pr-str [(System/getProperty \"foo\") (System/getProperty \"baz\")]))"
                                    temp-file-path))
      (is (= ["bar" "quux"] (edn/read-string  (slurp temp-file-path)))))))

(deftest tools-dir-env-test
  (fs/delete-tree "tools-dir")
  (try
    (let [[out err exit]
          (-> (process (str invoke-deps-cmd "-Sdescribe")
                       {:out :string
                        :err :string
                        :extra-env {"DEPS_CLJ_TOOLS_VERSION" "1.10.3.899"
                                    "DEPS_CLJ_TOOLS_DIR" "tools-dir"}})
              deref
              ((juxt :out :err :exit)))]
      (when-not (zero? exit)
        (println err))
      (is (= "1.10.3.899" (:version (edn/read-string out))))
      (is (str/includes? err "Clojure tools not yet in expected location:"))
      (is (fs/exists? (fs/file "tools-dir" "clojure-tools-1.10.3.899.jar")))
      (is (fs/exists? (fs/file "tools-dir" "example-deps.edn")))
      (is (fs/exists? (fs/file "tools-dir" "exec.jar")))
      (is (fs/exists? (fs/file "tools-dir" "tools.edn"))))
    (finally (fs/delete-tree "tools-dir"))))

(deftest without-cp-file-tests
  (doseq [[option output-contains]
          [["-Sdescribe" ":deps-clj-version"]
           ["-version" "Clojure CLI version (deps.clj)"]
           ["--help" "For more info, see:"]]]
    (testing (str option " doesn't create/use cp cache file")
      (try
        (let [{:keys [out exit]}
                                        ; use bogus deps-file to force using CLJ_CONFIG instead of current directory,
                                        ; meaning that the cache directory will be empty
              (-> (process (str invoke-deps-cmd "-Sdeps-file force_clj_config/missing.edn " option)
                           {:out :string
                            :err :string
                            :extra-env {"CLJ_CONFIG" "missing_config"}})
                  deref)]
          (is (empty? (fs/glob "missing_config" "**.cp" {:hidden true}))
              (str option " should not create a cp cache file"))
          (is (str/includes? out output-contains)
              (str option " output should contain '" output-contains "'"))
          (is (zero? exit)
              (str option " should have a zero exit code")))
        (finally (fs/delete-tree "missing_config"))))))

(deftest tools-test
  (deps/-main "-Ttools" "list"))

(defmacro get-shell-command-args
  "Executes BODY with the given ENV'ironment variables added to the
  `babashka.deps` scope, presumbably to indirectly invoke
  `babashka.deps/shell-command` whose invocation ARGS captures and
  returns with this call.

  It overrides `baabashka.deps/*exit-fn*` so as to never exit the
  program, but throws an exception in case of error while is still in
  the `babashka.deps` scope."
  [env-vars & body]
  (let [body-str (pr-str body)]
    `(let [shell-command# deps/shell-command
           ret*# (promise)
           sh-mock# (fn mock#
                      ([args#]
                       (mock# args# nil))
                      ([args# opts#]
                       (let [ret# (shell-command# args# opts#)]
                         (deliver ret*# args#)
                         ret#)))]
       ;; need to override both *process-fn* and deps/shell-command.
       (binding [deps/*process-fn* sh-mock#
                 deps/*exit-fn* (fn
                                  ([exit-code#] (when-not (= exit-code# 0)
                                                  (throw (ex-info "mock-shell-failed" {:exit-code exit-code#}))))
                                  ([exit-code# msg#] (throw  (ex-info "mock-shell-failed"
                                                                      {:exit-code exit-code# :msg msg#}))))
                 deps/*getenv-fn* #(or (get ~env-vars %)
                                      (System/getenv %))]
         (with-redefs [deps/shell-command sh-mock#]
           ~@body
           (or (deref ret*# 500 false) (ex-info "No shell-command invoked in body." {:body ~body-str})))))))

(deftest clj-jvm-opts+java-opts
  ;; The `CLJ_JVM_OPTS` env var should only apply to -P and -Spom.
  ;; The `JAVA_OPTS` env varshould only apply to everything else.
  ;;
  ;; Some harmless cli flags are used below to demonstrate the
  ;; succesful passing of cli arguments to the java executable.

  (let [xx-pclf "-XX:+PrintCommandLineFlags"
        xx-gc-threads "-XX:ConcGCThreads=1"]

    (testing "CLJ-JVM-OPTS with prepare deps"
        (let [sh-args (get-shell-command-args
                       {"CLJ_JVM_OPTS" (str/join " " [xx-pclf xx-gc-threads])}
                       (deps/-main "-P"))]
          (is (some #{xx-pclf} sh-args))
          ;; second and third args
          (is (= [xx-pclf xx-gc-threads] (->> (rest sh-args) (take 2))))))

    (testing "CLJ-JVM-OPTS with pom"
        (let [sh-args (get-shell-command-args
                       {"CLJ_JVM_OPTS" (str/join " " [xx-pclf xx-gc-threads])}
                       (deps/-main "-Spom"))]
          (is (some #{xx-pclf} sh-args))
          (is (= [xx-pclf xx-gc-threads] (->> (rest sh-args) (take 2))))))

    (testing "CLJ-JVM-OPTS outside of prepare deps"
        (let [sh-args (get-shell-command-args
                       {"CLJ_JVM_OPTS" xx-pclf}
                       (deps/-main "-e" "123"))]
          ;; shouldn't find the flag
          (is (not (some #{xx-pclf} sh-args)))))

    (testing "JAVA-OPTS outside of prepare deps"
      (let [sh-args (get-shell-command-args
                     {"JAVA_OPTS" (str/join " " [xx-pclf xx-gc-threads])}
                     (deps/-main "-e" "123"))]
        (is (some #{xx-pclf} sh-args))
        (is (= [xx-pclf xx-gc-threads] (->> (rest sh-args) (take 2))))))

    (testing "JAVA-OPTS with prepare deps"
      (let [sh-args (get-shell-command-args
                     {"JAVA_OPTS" xx-pclf}
                     (deps/-main "-P"))]
        (is (not (some #{xx-pclf} sh-args)))))))
