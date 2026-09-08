(ns clj-kondo.gen-macros.borkdude.deps-test
  (:require [borkdude.deps :as-alias deps]
            [babashka.fs :as-alias fs]))

(alias 'borkdude.deps-test 'clj-kondo.gen-macros.borkdude.deps-test)

(defmacro with-fresh-machine "Binds TOOLS-DIR and CONFIG-DIR under TEMP-DIR. Runs BODY with
  a classpath hook that skips installation." {:clj-kondo/macroexpand-hook true} [[temp-dir tools-dir config-dir] & body] `(let [~tools-dir (fs/file ~temp-dir "tools") ~config-dir (fs/file ~temp-dir "config")] (binding [deps/*getenv-fn* (fn [name#] (or (get {"DEPS_CLJ_TOOLS_DIR" (str ~tools-dir) "CLJ_CONFIG" (str ~config-dir)} name#) (System/getenv name#))) deps/*make-classpath-fn* (fn [{:keys [~'cmd ~'out]}] (deps/*aux-process-fn* {:cmd ~'cmd :out ~'out}))] ~@body)))
