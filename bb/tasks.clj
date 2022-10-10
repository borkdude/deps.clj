(ns tasks
  (:require [babashka.deps :as deps]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.string :as str]))

(defn compile-native
  "Compile library to standalone jar and a native executable program."
  []
  (let [graalvm-home (or (System/getenv "GRAALVM_HOME")
                         (throw (Exception. "Please set GRAALVM_HOME.")))
        java-home (str (fs/path graalvm-home "bin"))
        deps-clj-version (slurp "resources/DEPS_CLJ_VERSION")]
    (println "Building deps " deps-clj-version)
    (p/shell "lein" "deps.clj" "-Spath" "-Sdeps" "{:deps {borkdude/deps.clj {:mvn/version \"0.0.1\"}}}")
    (p/shell "lein with-profiles +native-image do clean, uberjar")
    (let [native-image (str (fs/path graalvm-home "bin"
                                     (if (fs/windows?) "native-image.cmd" "native-image")))]
      (println :graalvm-home graalvm-home :java-home java-home)
      (p/shell native-image "-jar" (format "target/deps.clj-%s-standalone.jar" deps-clj-version)
               "-H:Name=deps"
               "-H:+ReportExceptionStackTraces"
               "-J-Dclojure.spec.skip-macros=true"
               "-J-Dclojure.compiler.direct-linking=true"
               "-H:IncludeResources=DEPS_CLJ_VERSION"
               "--initialize-at-build-time"
               "-H:Log=registerResource:"
               "-H:EnableURLProtocols=http,https"
               "--enable-all-security-services"
               "--no-fallback"
               "--verbose"
               "--no-server"
               "-J-Xmx3g"))
    (p/shell "lein clean")
    (p/shell "./deps" "-Spath" "-Sdeps" "{:deps {borkdude/deps.clj {:mvn/version \"0.0.1\"}}}")))
