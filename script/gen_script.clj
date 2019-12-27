#!/usr/bin/env bb

(ns gen-script
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]))

(def raw-script (io/file "src" "borkdude" "deps.clj"))

(def script
  (str
   "#!/usr/bin/env bb --verbose\n\n"
   ";; Generated with script/gen_script.clj. Do not edit directly.\n\n"
   (str/replace (slurp raw-script) #"(?i)\s*\(:gen-class\)" "")
   "\n(apply -main *command-line-args*)\n"))

(spit "deps.clj" script)
(System/exit (:exit (sh "chmod" "+x" "deps.clj")))
