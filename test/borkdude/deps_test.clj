(ns borkdude.deps-test
  (:require
   [babashka.fs :as fs]
   [borkdude.deps :as deps]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :as t :refer [deftest is testing]]))

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
      (is (= "6" out)))))

(deftest jvm-proxy-settings-test
  (is (= {:host "aHost" :port "1234"} (deps/parse-proxy-info "http://aHost:1234")))
  (is (= {:host "aHost" :port "1234"} (deps/parse-proxy-info "http://user:pw@aHost:1234")))
  (is (= {:host "aHost" :port "1234"} (deps/parse-proxy-info "https://aHost:1234")))
  (is (= {:host "aHost" :port "1234"} (deps/parse-proxy-info "https://user:pw@aHost:1234")))
  (is (nil? (deps/parse-proxy-info "http://aHost:abc"))))

(deftest jvm-opts-test
  (let [temp-dir (fs/create-temp-dir)
        temp-file (fs/create-file (fs/path temp-dir "temp.txt"))
        temp-file-path (str temp-file)]
    (deps/-main "-J-Dfoo=bar" "-J-Dbaz=quux"
                "-M" "-e" (format "
(spit \"%s\" (pr-str [(System/getProperty \"foo\") (System/getProperty \"baz\")]))"
                                  temp-file-path))
    (is (= ["bar" "quux"] (edn/read-string  (slurp temp-file-path))))))
