#!/usr/bin/env bb

(ns gen-script
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]))

(def raw-script (io/file "src" "borkdude" "deps.clj"))
(def version (-> (io/file "resources" "DEPS_CLJ_VERSION")
                 (slurp)
                 (str/trim)))

(def version-code "(def deps-clj-version
  (-> (io/resource \"DEPS_CLJ_VERSION\")
      (slurp)
      (str/trim)))")

(def script
  (str
   "#!/usr/bin/env bb --verbose\n\n"
   ";; Generated with script/gen_script.clj. Do not edit directly.\n\n"
   (-> (slurp raw-script)
       (str/replace #"(?i)\s*\(:gen-class\)" "")
       (str/replace version-code (format "(def deps-clj-version %s)" (pr-str version))))
   "\n(apply -main *command-line-args*)\n"))

(spit "deps.clj" script)
(System/exit (:exit (sh "chmod" "+x" "deps.clj")))
