(ns hooks.deps-test
  (:require [clj-kondo.hooks-api :as api]))

(defn with-fresh-machine
  "(with-fresh-machine [temp-dir tools-dir config-dir] body ...) binds
  tools-dir and config-dir from temp-dir, so it lints as that let."
  [{:keys [node]}]
  (let [[_ binding-vec & body] (:children node)
        [temp-dir tools-dir config-dir] (:children binding-vec)]
    {:node (api/list-node
            (list* (api/token-node 'let)
                   (api/vector-node [tools-dir temp-dir config-dir temp-dir])
                   body))}))
