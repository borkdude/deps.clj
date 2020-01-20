(ns borkdude.deps-test
  (:require
   [spartan.test :as t :refer [deftest is]]
   [borkdude.deps :as deps]
   [clojure.string :as str]
   [clojure.edn :as edn]))

(deftest path-test
  (is (str/includes? (with-out-str
                       (deps/-main "-Spath")) "resources")))

(deftest describe-test
  (let [{:keys [:config-files :config-user :config-project :deps-clj-version :version]}
        (edn/read-string (with-out-str
                           (deps/-main "-Sdescribe")))]
    (is (every? some? [config-files config-user config-project deps-clj-version version]))))

(deftest tree-test
  (is (str/includes?
       (with-out-str
         (deps/-main "-Stree"))
       "org.clojure/clojure")))

(deftest jvm-proxy-settings-test
  ;; TODO:
  #_(is (= "..." (deps/parse-proxy-info "..."))))
