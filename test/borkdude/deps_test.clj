(ns borkdude.deps-test
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [check process]]
   [borkdude.deps :as deps]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]])
  (:import (java.util.jar Attributes$Name JarEntry JarOutputStream Manifest)
           [java.util.zip ZipEntry ZipOutputStream]))

;; Print out information about the java executable that will be used
;; by deps.clj, useful for user validation.
(let [java (#'deps/get-java-cmd)
      version (-> (process [java "-version"] {:err :string})
                  check
                  :err str/split-lines first)]
  (def java-version version)
  (println :deps.clj/java :path (pr-str java) :version version))

(defn invoke-deps-cmd
  "Returns the command string that can be used to invoke the
  `borkdude.deps/-main` fn with the given ARGS from the command line.

  The tool to use for invoking the fn is selected based on the value
  of the `DEPS_CLJ_TEST_ENV` env variable, which can be one
  of (defaults to `clojure`):

  `babashka`: Use `bb`.
  `clojure`: Use the `clojure` cli tool.
  `native`:  Use the `deps` native binary."
  [& args]

  (case (or (System/getenv "DEPS_CLJ_TEST_ENV") "clojure")
    "babashka" (let [classpath (str/join @#'deps/path-separator ["src" "test" "resources"])]
                 (apply str "bb -cp " classpath " -m borkdude.deps " args))
    "native" (apply str "./deps " args)
    "clojure" (cond->>
               (apply str "clojure -M -m borkdude.deps " args)
                @#'deps/windows?
                ;; the `exit` command is a workaround for
                ;; https://ask.clojure.org/index.php/12290/clojuretools-commands-windows-properly-exit-code-failure
                (format "powershell -NoProfile -Command %s; exit $LASTEXITCODE"))))

(deftest parse-cli-opts-test
  (is (= {:mode :repl, :jvm-opts ["-Dfoo=bar" "-Dbaz=quuz"]}
         (deps/parse-cli-opts ["-J-Dfoo=bar" "-J-Dbaz=quuz"])))
  (is (= {:mode :main, :main-aliases nil, :args '("-e" "(+ 1 2 3)")}
         (deps/parse-cli-opts ["-M" "-e" "(+ 1 2 3)"])))
  (is (= {:mode :main, :main-aliases ":foo", :args nil} (deps/parse-cli-opts ["-M:foo"]))))

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

(defmacro deps-main-throw
  "Same as `babashka.deps/-main`, but throws an exception on error
  instead of exiting the process.

  The exception's additional data map keys are:

  :exit-code The process's exit code.

  :msg The process's error messsage (if any)."
  [& command-line-args]
  `(binding [deps/*exit-fn*
             (fn [{:keys [~'exit ~'message]}]
               (when ~'message
                 (throw (ex-info (str ::deps-main-throw)
                                 {:exit-code ~'exit :msg ~'message}))))]
     (deps/-main ~@command-line-args)))

(deftest whitespace-test
  (testing "jvm opts"
    (let [temp-dir (fs/create-temp-dir)
          temp-file (fs/create-file (fs/path temp-dir "temp.txt"))
          temp-file-path (str temp-file)
          _ (deps-main-throw "-Sdeps" "{:aliases {:space {:jvm-opts [\"-Dfoo=foo bar\"]}}}" "-M:space" "-e"
                             (format "(spit \"%s\" (System/getProperty \"foo\"))"
                                     (.toURI (fs/file temp-file-path))))
          out (slurp temp-file-path)]
      (is (= "foo bar" out))))
  (testing "main opts"
    (let [temp-dir (fs/create-temp-dir)
          temp-file (fs/create-file (fs/path temp-dir "temp.txt"))
          temp-file-path (str temp-file)
          _ (deps-main-throw "-Sdeps"
                             (format
                              (if-not @#'deps/windows?
                                "{:aliases {:space {:main-opts [\"-e\" \"(spit \\\"%s\\\" (+ 1 2 3))\"]}}}"
                                "{:aliases {:space {:main-opts [\"-e\" \"(spit \\\\\"%s\\\\\" (+ 1 2 3))\"]}}}")
                              (.toURI (fs/file temp-file-path)))
                             "-M:space")
          out (slurp temp-file-path)]
      (is (= "6" out)))))

(deftest jvm-proxy-settings-test
  (is (= {:host "aHost" :port "1234"} (#'deps/parse-proxy-info "http://aHost:1234")))
  (is (= {:host "aHost" :port "1234"} (#'deps/parse-proxy-info "http://user:pw@aHost:1234")))
  (is (= {:host "aHost" :port "1234"} (#'deps/parse-proxy-info "https://aHost:1234")))
  (is (= {:host "aHost" :port "1234"} (#'deps/parse-proxy-info "https://user:pw@aHost:1234")))
  (is (nil? (#'deps/parse-proxy-info "http://aHost:abc")))
  (is (= ["host1" "host2"] (#'deps/parse-noproxy-list "host1,host2")))
  (is (= {:http-proxy {:host "aHost" :port "1234"}}
         (binding [deps/*getenv-fn* {"http_proxy" "http://aHost:1234"}]
           (deps/get-proxy-info))))
  (is (= {:http-proxy {:host "aHost" :port "1234"}}
         (binding [deps/*getenv-fn* {"HTTP_PROXY" "http://aHost:1234"}]
           (deps/get-proxy-info))))
  (is (= {:https-proxy {:host "aHost" :port "1234"}}
         (binding [deps/*getenv-fn* {"https_proxy" "http://aHost:1234"}]
           (deps/get-proxy-info))))
  (is (= {:https-proxy {:host "aHost" :port "1234"}}
         (binding [deps/*getenv-fn* {"HTTPS_PROXY" "http://aHost:1234"}]
           (deps/get-proxy-info))))
  (is (= {}
         (binding [deps/*getenv-fn* {}]
           (deps/get-proxy-info))))
  (is (= {}
         (binding [deps/*getenv-fn* {"http_proxy" "http://aHost:abc"}]
           (deps/get-proxy-info))))
  (is (= {:no-proxy ["host1" "host2"]}
         (binding [deps/*getenv-fn* {"no_proxy" "host1,host2"}]
           (deps/get-proxy-info))))
  (is (= {:no-proxy ["host1" "host2"]}
         (binding [deps/*getenv-fn* {"NO_PROXY" "host1,host2"}]
           (deps/get-proxy-info)))))

(deftest jvm-opts-test
  (let [temp-dir (fs/create-temp-dir)
        temp-file (fs/create-file (fs/path temp-dir "temp.txt"))
        temp-file-path (str temp-file)]
    (deps-main-throw "-J-Dfoo=bar" "-J-Dbaz=quux"
                     "-M" "-e" (format "
(spit \"%s\" (pr-str [(System/getProperty \"foo\") (System/getProperty \"baz\")]))"
                                       (.toURI (fs/file temp-file-path))))
    (is (= ["bar" "quux"] (edn/read-string (slurp temp-file-path))))))

(deftest tools-dir-env-test
  (doseq [version ["1.10.3.899" "1.11.1.1386"]]
    (fs/delete-tree "tools-dir")
    (try
      (let [[out err]
            (-> (process (invoke-deps-cmd "-Sdescribe")
                         {:out :string
                          :err :string
                          :extra-env {"DEPS_CLJ_TOOLS_VERSION" version
                                      "DEPS_CLJ_TOOLS_DIR" "tools-dir"}})
                check
                ((juxt :out :err)))]
        (println err)
        (is (= version (:version (edn/read-string out))))
        (is (str/includes? err "Clojure tools not yet in expected location:"))
        (is (fs/exists? (fs/file "tools-dir" (format "clojure-tools-%s.jar" version))))
        (is (fs/exists? (fs/file "tools-dir" "example-deps.edn")))
        (is (fs/exists? (fs/file "tools-dir" "exec.jar")))
        (is (fs/exists? (fs/file "tools-dir" "tools.edn"))))
      (finally (fs/delete-tree "tools-dir")))))

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
              (-> (process (invoke-deps-cmd "-Sdeps-file force_clj_config/missing.edn " option)
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
  "Executes BODY with the given extra ENV-VARS environment variables
  added to the `babashka.deps` scope, presumbably to indirectly invoke
  `babashka.deps/shell-command` whose invocation ARGS it captures and
  returns with this call.

  It overrides `baabashka.deps/*exit-fn*` so as to never exit the
  program, but throws an exception in case of error while is still in
  the `babashka.deps` scope."
  [env-vars & body]
  (let [body-str (pr-str body)]
    `(let [shell-command# @#'deps/internal-shell-command
           ret*# (promise)
           sh-mock# (fn mock#
                      ([args#]
                       (mock# args# nil))
                      ([args# opts#]
                       (let [ret# (shell-command# args# opts#)]
                         (deliver ret*# args#)
                         ret#)))]
       ;; need to override both *process-fn* and deps/shell-command.
       (binding [deps/*clojure-process-fn* (fn ~'[{:keys [cmd]}]
                                             (sh-mock# ~'cmd))
                 deps/*aux-process-fn* (fn ~'[{:keys [cmd out]}]
                                         (sh-mock# ~'cmd {:to-string? (= :string ~'out)}))
                 deps/*exit-fn* (fn [{:keys [~'exit ~'message]}]
                                  (when ~'message
                                    (throw (ex-info "mock-shell-failed"
                                                    {:exit-code ~'exit :msg ~'message}))))
                 deps/*getenv-fn* #(or (get ~env-vars %)
                                       (System/getenv %))]
         ~@body
         (or (deref ret*# 500 false) (ex-info "No shell-command invoked in body." {:body ~body-str}))))))

(defn java-major-version-get
  "Returns the major version number of the java executable used to run
  the java commands at run time. For implementation simplicity the
  major version before java 11 is returned as 1."
  []
  (-> (process [(#'deps/get-java-cmd) "-version"] {:err :string})
      check
      :err
      (->> (re-find #"version \"(\d+)"))
      second
      Integer/parseInt))

(defn clojure-tools-dummy-zip-file-create
  "Creates a dummy clojure tools zip file in OUT-DIR and returns its
  path."
  [out-dir]
  (let [{:keys [ct-base-dir ct-aux-files-names ct-jar-name]} @@#'deps/clojure-tools-info*
        file (io/file out-dir "borkdude-deps-test-dummy-tools.zip")]
    (with-open [os (io/output-stream file)
                zip (ZipOutputStream. os)]
      (doseq [entry (into [ct-jar-name] ct-aux-files-names)]
        (doto zip
          (.putNextEntry (ZipEntry. (str ct-base-dir "/" entry)))
          (.write (.getBytes "{:lib io.github.clojure/tools.tools
 :coord {:git/tag \"v0.3.4\"
         :git/sha \"0e9e6c8b409ac916ad6f2ec5bc075bbcb09545c0\"}}"))
          (.closeEntry)))
      file)))

(deftest clojure-tools-download-test
  ;; Test clojure tools download methods
  ;;
  ;; - via java subprocess, when CLJ_JVM_OPTS (requires java11+).
  ;; - direct download, when the above is not ran or fails.
  ;; - custom download using user supplied function
  ;; - manual download, simulating a user following manual instructions.
  (let [java-version (java-major-version-get)
        {:keys [ct-error-exit-code ct-jar-name ct-url-str ct-zip-name]} @@#'deps/clojure-tools-info*]
    (testing "java downloader"
      (fs/with-temp-dir
        [temp-dir {}]
        (let [tools-zip (clojure-tools-dummy-zip-file-create (str temp-dir))
              url (io/as-url tools-zip)
              dest-zip-file (fs/file temp-dir ct-zip-name)]
          (if (< java-version 11)
            ;; requires java11+, fails otherwise
            (do (is (= false (#'deps/clojure-tools-download-java! {:url url :dest dest-zip-file})))
                (is (not (fs/exists? dest-zip-file))))

            (do (is (= true (#'deps/clojure-tools-download-java! {:url url :dest dest-zip-file})))
                (is (fs/exists? dest-zip-file)))))))

    (when (>= java-version 11)
      (testing "java downloader called from -main when CLJ_JVM_OPTS is set"
        (fs/with-temp-dir
          [temp-dir {}]
          (let [dest-jar-file (fs/file temp-dir ct-jar-name)]
            (with-redefs [deps/clojure-tools-download-direct!
                          (fn [& _] (throw (Exception. "Direct should not be called.")))]
              (let [xx-pclf "-XX:+PrintCommandLineFlags"
                    xx-gc-threads "-XX:ConcGCThreads=1"
                    sh-args (get-shell-command-args
                             {"DEPS_CLJ_TOOLS_DIR" (str temp-dir)
                              "CLJ_JVM_OPTS" (str/join " " [xx-pclf xx-gc-threads])}
                             (deps/-main "--version"))]
                (is (some #{xx-pclf} sh-args))
                ;; second and third args
                (is (set/subset? #{xx-pclf xx-gc-threads} (->> (rest sh-args) set))))
              (is (fs/exists? dest-jar-file)))))))

    (testing "direct downloader"
      (fs/with-temp-dir
        [temp-dir {}]
        (let [url-str ct-url-str
              dest-zip-file (fs/file temp-dir ct-zip-name)]
          (is (= true (deps/clojure-tools-download-direct! {:url url-str :dest dest-zip-file})))
          (is (fs/exists? dest-zip-file)))))

    (testing "direct downloader called from -main (CLJ_JVM_OPTS not set)"
      (fs/with-temp-dir
        [temp-dir {}]
        (let [dest-jar-file (fs/file temp-dir ct-jar-name)]
          (with-redefs [deps/clojure-tools-download-java!
                        (fn [& _] (throw (Exception. "Java subprocess should not be called.")))]
            (binding [deps/*getenv-fn* #(or (get {"DEPS_CLJ_TOOLS_DIR" (str temp-dir)
                                                  "CLJ_JVM_OPTS" nil} %)
                                            (System/getenv %))]

              (deps-main-throw "--version")
              (is (fs/exists? dest-jar-file)))))))

    (testing "manual user installation"
      (fs/with-temp-dir
        [temp-dir {}]
        (let [tools-zip-file (clojure-tools-dummy-zip-file-create (str temp-dir))
              dest-zip-file (fs/file temp-dir ct-zip-name)
              dest-jar-file (fs/file temp-dir ct-jar-name)]
          (fs/copy tools-zip-file dest-zip-file) ;; user copies downloaded file
          (with-redefs [deps/clojure-tools-download-java!
                        (fn [& _] (throw (Exception. "Java should not be called.")))
                        deps/clojure-tools-download-direct!
                        (fn [& _] (throw (Exception. "Direct should not be called.")))]
            (binding [deps/*getenv-fn* #(or (get {"DEPS_CLJ_TOOLS_DIR" (str temp-dir)} %)
                                            (System/getenv %))]

              (deps-main-throw "--version")
              (is (fs/exists? dest-jar-file)))))))

    (testing "custom user function"
      (fs/with-temp-dir
        [temp-dir {}]
        (let [dest-jar-file (fs/file temp-dir ct-jar-name)]
          (with-redefs [deps/clojure-tools-download-java!
                        (fn [& _] (throw (Exception. "Java should not be called.")))
                        deps/clojure-tools-download-direct!
                        (fn [& _] (throw (Exception. "Direct should not be called.")))]
            (binding [deps/*getenv-fn* #(or (get {"DEPS_CLJ_TOOLS_DIR" (str temp-dir)} %)
                                            (System/getenv %))
                      deps/*clojure-tools-download-fn*
                      (fn [{:keys [url dest] :as opts}]
                        ; Simulate download by creating dummy file and copying to
                        ; specified destination location
                        (is (str/starts-with? url "http"))
                        (is (contains? opts :clj-jvm-opts))
                        (is (contains? opts :proxy-opts))
                        (let [tools-zip-file (clojure-tools-dummy-zip-file-create (str temp-dir))
                              dest-zip-file (fs/file dest)]
                          (fs/copy tools-zip-file dest-zip-file)
                          true))]
              (deps-main-throw "--version")
              (is (fs/exists? dest-jar-file)))))))

    (testing "prompt for manual user install"
      (fs/with-temp-dir
        [temp-dir {}]
        (with-redefs [deps/clojure-tools-download-java!
                      (fn [& _] false)
                      deps/clojure-tools-download-direct!
                      (fn [& _] false)]
          (binding [deps/*getenv-fn* #(or (get {"DEPS_CLJ_TOOLS_DIR" (str temp-dir)} %)
                                          (System/getenv %))]

            (let [exit-data* (atom {})]
              (try (deps-main-throw "--version")
                   (catch Exception e
                     (reset! exit-data* (ex-data e))))
              (let [exit-data @exit-data*]
                (is (= ct-error-exit-code (:exit-code exit-data)) exit-data)))))))))

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
        (is (set/subset? #{xx-pclf xx-gc-threads} (->> (rest sh-args) set)))))

    (testing "CLJ-JVM-OPTS with pom"
      (let [sh-args (get-shell-command-args
                     {"CLJ_JVM_OPTS" (str/join " " [xx-pclf xx-gc-threads])}
                     (deps/-main "-Spom"))]
        (is (some #{xx-pclf} sh-args))
        (is (set/subset? #{xx-pclf xx-gc-threads} (->> (rest sh-args) set)))))

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
        (is (set/subset? #{xx-pclf xx-gc-threads} (->> (rest sh-args) set)))))

    (testing "JAVA-OPTS with prepare deps"
      (let [sh-args (get-shell-command-args
                     {"JAVA_OPTS" xx-pclf}
                     (deps/-main "-P"))]
        (is (not (some #{xx-pclf} sh-args)))))))

(deftest stale-cache-rm-mvn-dir-test
  (let [deps-map (pr-str '{:mvn/local-repo "test/mvn" :deps {medley/medley {:mvn/version "1.4.0"}
                                                             io.github.borkdude/quickblog {:git/sha "8f5898ee911101a96295f59bb5ffc7517757bc8f"}}})
        delete #(do (fs/delete-tree (fs/file "test" "mvn"))
                    (fs/delete-tree (fs/file (or (some-> (System/getenv "GITLIBS") (fs/file))
                                                 (fs/file (System/getProperty "user.dir" ".gitlibs")))
                                             "libs" "io.github.borkdude/quickblog" "8f5898ee911101a96295f59bb5ffc7517757bc8f")))
        test #(deps/-main "-Sdeps" deps-map "-M" "-e" "(require '[medley.core]) (require '[quickblog.api])")]
    (delete)
    (test)
    (delete)
    (test)))

(deftest get-basis-file-test
  (binding [deps/*exit-fn* (constantly nil)]
    (deps/-main "-Sdeps-file" "test/other-deps.edn" "-P"))
  (let [deps-edn (deps/get-local-deps-edn
                  {:cli-opts (deps/parse-cli-opts ["-Sdeps-file" "test/other-deps.edn"])})
        config-dir (deps/get-config-dir)
        install-dir (deps/get-install-dir)
        basis (-> (deps/get-basis-file {:cache-dir
                                        (deps/get-cache-dir
                                         {:deps-edn deps-edn
                                          :config-dir config-dir})
                                        :checksum (deps/get-checksum
                                                   {:cli-opts []
                                                    :config-paths
                                                    (deps/get-config-paths {:cli-opts []
                                                                            :deps-edn deps-edn
                                                                            :config-dir config-dir
                                                                            :install-dir install-dir})})})
                  slurp
                  edn/read-string)]
    (is
     (set/subset?
      (-> basis
          keys
          set)
      (set '(:paths :deps :aliases :mvn/repos :libs :classpath-roots :classpath :basis-config))))
    #_#_(require 'clojure.pprint)
      ((requiring-resolve 'clojure.pprint/pprint) basis)
    (is (contains? (:libs basis) 'medley/medley))))

(deftest long-classpath-test
  (when-not (str/includes? java-version "1.8.0")
    (let [prev-cp (str/trim (with-out-str (borkdude.deps/-main "-Spath")))
          long-cp (str/join fs/path-separator (cons prev-cp (repeat 15000 "src")))
          ret (atom nil)]
      (binding [deps/*exit-fn* #(reset! ret %)]
        (borkdude.deps/-main "-Scp" long-cp "-M" "-e" "nil"))
      (is (or (nil? @ret)
              (zero? (:exit @ret)))))))

(deftest resolve-in-dir-test
  (let [tmp-dir (System/getProperty "java.io.tmpdir")
        home-dir (System/getProperty "user.home")]
    (is (str/starts-with? (#'borkdude.deps/resolve-in-dir tmp-dir "dude") tmp-dir))
    (is (= home-dir (#'borkdude.deps/resolve-in-dir tmp-dir home-dir)))))


(defn- is-make-classpath? [args]
  (and (some #(= "clojure.main" %) args)
       (some (fn [[a b]]
               (and (= a "-m")
                    (= b "clojure.tools.deps.script.make-classpath2")))
             (partition 2 1 args))))


(deftest issue-101
  (let [temp-dir (fs/create-temp-dir)
        temp-jar (fs/create-file (fs/path temp-dir "temp.jar"))
        deps-file (fs/create-file (fs/path temp-dir "deps.edn"))
        manifest (Manifest.)
        attributes (.getMainAttributes manifest)]
    (.mkdirs (fs/file temp-dir))
    (.put attributes Attributes$Name/MANIFEST_VERSION "1.0")
    (with-open [stream (JarOutputStream. (io/output-stream (fs/file temp-jar)) manifest)]
      (.putNextEntry stream (JarEntry. "test.txt"))
      (try
        (.write stream (.getBytes "Hello World\n"))
        (finally
          (.closeEntry stream))))
    (spit (fs/file deps-file) (pr-str '{:paths ["temp.jar"]}))
    (let [classpath-created-count (atom 0)]
      (binding [deps/*exit-fn* (constantly nil)
                deps/*dir* (str temp-dir)
                deps/*aux-process-fn* (fn [{:keys [cmd out]}]
                                        (when (is-make-classpath? cmd)
                                          (swap! classpath-created-count inc))
                                        (#'deps/internal-shell-command cmd {:out out}))]
        (deps/-main "-Spath")
        (deps/-main "-Spath") ; Should not recalculate classpath the second time
        (is (= @classpath-created-count 1))))))
