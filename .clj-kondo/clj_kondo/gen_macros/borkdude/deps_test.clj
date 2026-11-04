(ns clj-kondo.gen-macros.borkdude.deps-test
  (:require [borkdude.deps :as-alias deps]
            [babashka.fs :as-alias fs]))

(alias 'borkdude.deps-test 'clj-kondo.gen-macros.borkdude.deps-test)

(let [created? (nil? (find-ns 'babashka.process))]
  (intern (create-ns 'babashka.process) 'check)
  (intern (create-ns 'babashka.process) 'process)
  (refer 'babashka.process :only '[check process])
  (when created? (remove-ns 'babashka.process)))

(let [created? (nil? (find-ns 'clojure.test))]
  (intern (create-ns 'clojure.test) 'deftest)
  (intern (create-ns 'clojure.test) 'is)
  (intern (create-ns 'clojure.test) 'testing)
  (refer 'clojure.test :only '[deftest is testing])
  (when created? (remove-ns 'clojure.test)))

(defmacro with-fresh-machine "Binds TOOLS-DIR and CONFIG-DIR under TEMP-DIR. Runs BODY in TEMP-DIR with
  a classpath hook that skips installation." {:clj-kondo/macroexpand-hook true} [[temp-dir tools-dir config-dir] & body] `(let [~tools-dir (fs/file ~temp-dir "tools") ~config-dir (fs/file ~temp-dir "config") cache-dir# (fs/create-dirs (fs/file ~temp-dir "cache"))] (binding [deps/*dir* (str ~temp-dir) deps/*getenv-fn* (fn [name#] (or (get {"DEPS_CLJ_TOOLS_DIR" (str ~tools-dir) "CLJ_CONFIG" (str ~config-dir) "CLJ_CACHE" (str cache-dir#)} name#) (System/getenv name#))) deps/*make-classpath-fn* (fn [{:keys [~'cmd ~'out]}] (deps/*aux-process-fn* {:cmd ~'cmd :out ~'out}))] ~@body)))
