#!/usr/bin/env bb

(ns gen-script
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]))

(def raw-script (io/file "src" "borkdude" "deps.clj"))

(def script
  (str
   "#!/usr/bin/env bb --verbose\n\n"
   (slurp raw-script)
   "\n(apply -main *command-line-args*)\n"))

(spit "deps.clj" script)
(System/exit (:exit (sh "chmod" "+x" "deps.clj")))
