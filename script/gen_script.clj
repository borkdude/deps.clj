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
  (str ";; Generated with script/gen_script.clj. Do not edit directly.\n\n"
       (-> (slurp raw-script)
           (str/replace #"(?i)\s*\(:gen-class\)" "")
           (str/replace version-code (format "(def deps-clj-version %s)" (pr-str version))))
       "\n(apply -main *command-line-args*)\n"))

(def clj-script
  (str
   "#!/usr/bin/env bb\n\n"
   script))

(spit "deps.clj" clj-script)
(sh "chmod" "+x" "deps.clj")

(def bat-shebang
  (str "@SETLOCAL
@SET BABASHKA_SKIPLINES= ^
 (binding [*file* (first *command-line-args*) ^
           *command-line-args*,(next,*command-line-args*)] ^
  (load-string (str/join \\newline (drop 8 (str/split-lines (slurp *file*))))))
@ENDLOCAL
@SETLOCAL ENABLEDELAYEDEXPANSION & bb -e \"%BABASHKA_SKIPLINES%\" \"%~f0\" %* & EXIT /B !ERRORLEVEL!

"))

(def bat-script
  (str bat-shebang
       script))

(spit "deps.bat" bat-script)
