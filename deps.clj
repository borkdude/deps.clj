#!/usr/bin/env bb

;; Generated with script/gen_script.clj. Do not edit directly.

(ns borkdude.deps
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io])
  (:import [java.lang ProcessBuilder$Redirect]
           [java.net URL HttpURLConnection]
           [java.nio.file Files FileSystems CopyOption]))

(set! *warn-on-reflection* true)

(def version "1.10.1.507")
(def deps-clj-version "0.0.9")

(defn shell-command
  "Executes shell command.

  Accepts the following options:

  `:input`: instead of reading from stdin, read from this string.

  `:to-string?`: instead of writing to stdoud, write to a string and
  return it.

  `:throw?`: Unless `false`, exits script when the shell-command has a
  non-zero exit code, unless `throw?` is set to false."
  ([args] (shell-command args nil))
  ([args {:keys [:input :to-string? :throw? :show-errors?]
          :or {throw? true
               show-errors? true}}]
   (let [args (mapv str args)
         pb (cond-> (ProcessBuilder. ^java.util.List args)
              show-errors? (.redirectError ProcessBuilder$Redirect/INHERIT)
              (not to-string?) (.redirectOutput ProcessBuilder$Redirect/INHERIT)
              (not input) (.redirectInput ProcessBuilder$Redirect/INHERIT))
         proc (.start pb)]
     (when input
       (with-open [w (io/writer (.getOutputStream proc))]
         (binding [*out* w]
           (print input)
           (flush))))
     (let [string-out
           (when to-string?
             (let [sw (java.io.StringWriter.)]
               (with-open [w (io/reader (.getInputStream proc))]
                 (io/copy w sw))
               (str sw)))
           exit-code (.waitFor proc)]
       (when (and throw? (not (zero? exit-code)))
         (System/exit exit-code))
       string-out))))

(def help-text (str "Version: " version "

Usage: clojure [dep-opt*] [--] [init-opt*] [main-opt] [arg*]
       clj     [dep-opt*] [--] [init-opt*] [main-opt] [arg*]

The clojure script is a runner for Clojure. clj is a wrapper
for interactive repl use. These scripts ultimately construct and
invoke a command-line of the form:

java [java-opt*] -cp classpath clojure.main [init-opt*] [main-opt] [arg*]

The dep-opts are used to build the java-opts and classpath:
 -Jopt          Pass opt through in java_opts, ex: -J-Xmx512m
 -Oalias...     Concatenated jvm option aliases, ex: -O:mem
 -Ralias...     Concatenated resolve-deps aliases, ex: -R:bench:1.9
 -Calias...     Concatenated make-classpath aliases, ex: -C:dev
 -Malias...     Concatenated main option aliases, ex: -M:test
 -Aalias...     Concatenated aliases of any kind, ex: -A:dev:mem
 -Sdeps EDN     Deps data to use as the last deps file to be merged
 -Spath         Compute classpath and echo to stdout only
 -Scp CP        Do NOT compute or cache classpath, use this one instead
 -Srepro        Ignore the ~/.clojure/deps.edn config file
 -Sforce        Force recomputation of the classpath (don't use the cache)
 -Spom          Generate (or update existing) pom.xml with deps and paths
 -Stree         Print dependency tree
 -Sresolve-tags Resolve git coordinate tags to shas and update deps.edn
 -Sverbose      Print important path info to console
 -Sdescribe     Print environment and command parsing info as data
 -Sthreads      Set specific number of download threads
 -Strace        Write a trace.edn file that traces deps expansion
 --             Stop parsing dep options and pass remaining arguments to clojure.main

The following non-standard options are added:

 -Sdeps-file    Use this file instead of deps.edn
 -Scommand      A custom command that will be invoked. Substitutions: {{classpath}}, {{main-opts}}.

init-opt:
 -i, --init path     Load a file or resource
 -e, --eval string   Eval exprs in string; print non-nil values
 --report target     Report uncaught exception to \"file\" (default), \"stderr\", or \"none\",
                     overrides System property clojure.main.report

main-opt:
 -m, --main ns-name  Call the -main function from namespace w/args
 -r, --repl          Run a repl
 path                Run a script from a file or resource
 -                   Run a script from standard input
 -h, -?, --help      Print this help message and exit

For more info, see:
 https://clojure.org/guides/deps_and_cli
 https://clojure.org/reference/repl_and_main
"))

(def parse-opts->keyword
  {"-J" :jvm-opts
   "-R" :resolve-aliases
   "-C" :classpath-aliases
   "-O" :jvm-aliases
   "-M" :main-aliases
   "-A" :all-aliases})

(def bool-opts->keyword
  {"-Spath" :print-classpath
   "-Sverbose" :verbose
   "-Strace" :trace
   "-Sdescribe" :describe
   "-Sforce" :force
   "-Srepro" :repro
   "-Stree" :tree
   "-Spom" :pom
   "-Sresolve-tags" :resolve-tags})

(def string-opts->keyword
  {"-Sdeps" :deps-data
   "-Scp" :force-cp
   "-Sdeps-file" :deps-file
   "-Scommand" :command
   "-Sthreads" :threads})

(defn describe-line [[kw val]]
  (pr kw val ))

(defn describe [lines]
  (let [[first-line & lines] lines]
    (print "{") (describe-line first-line)
    (doseq [line lines
            :when line]
      (print "\n ") (describe-line line))
    (println "}")))

(defn windows? []
  (-> (System/getProperty "os.name")
      (str/lower-case)
      (str/includes? "windows")))

(defn double-quote
  "Double quotes shell arguments on Windows. On other platforms it just
  passes through the string."
  [s]
  (if (windows?)
    (format "\"\"%s\"\"" s)
    s))

(defn cksum
  [^String s]
  (let [hashed (.digest (java.security.MessageDigest/getInstance "MD5")
                        (.getBytes s))
        sw (java.io.StringWriter.)]
    (binding [*out* sw]
      (doseq [byte hashed]
        (print (format "%02X" byte))))
    (str sw)))

(defn which [executable]
  (let [path (System/getenv "PATH")
        paths (.split path (System/getProperty "path.separator"))]
    (loop [paths paths]
      (when-first [p paths]
        (let [f (io/file p executable)]
          (if (and (.isFile f)
                   (.canExecute f))
            (.getCanonicalPath f)
            (recur (rest paths))))))))

(defn home-dir []
  (if (windows?)
    ;; workaround for https://github.com/oracle/graal/issues/1630
    (System/getenv "userprofile")
    (System/getProperty "user.home")))

(defn download [source dest]
  (let [source (URL. source)
        dest (io/file dest)
        conn ^HttpURLConnection (.openConnection ^URL source)]
    (.setInstanceFollowRedirects conn true)
    (.connect conn)
    (with-open [is (.getInputStream conn)]
      (io/copy is dest))))

(def clojure-tools-jar (format "clojure-tools-%s.jar" version))

(defn unzip [zip-file destination-dir]
  (let [zip-file (io/file zip-file)
        _ (.mkdirs (io/file destination-dir "ClojureTools"))
        fs (FileSystems/newFileSystem (.toPath zip-file) nil)]
    (doseq [f [clojure-tools-jar "example-deps.edn"]]
      (let [file-in-zip (.getPath fs "ClojureTools" (into-array String [f]))]
        (Files/copy file-in-zip (.toPath (io/file destination-dir "ClojureTools" f))
                    ^{:tag "[Ljava.nio.file.CopyOption;"} (into-array CopyOption []))))))

(defn clojure-tools-jar-download
  "Downloads clojure tools jar into deps-clj-config-dir."
  [^java.io.File deps-clj-config-dir]
  (let [zip (io/file deps-clj-config-dir "tools.zip")]
    (.mkdirs deps-clj-config-dir)
    (download (format "https://download.clojure.org/install/clojure-tools-%s.zip" version)
              zip)
    (unzip zip (.getPath deps-clj-config-dir))
    (.delete zip)))

(def ^:private authenticated-proxy-re #".+:.+@(.+):(\d+).*")
(def ^:private unauthenticated-proxy-re #"(.+):(\d+).*")

(defn parse-proxy-info
  [s]
  (when s
    (let [p (cond
              (clojure.string/starts-with? s "http://") (subs s 7)
              (clojure.string/starts-with? s "https://") (subs s 8)
              :else s)
          auth-proxy-match (re-matches authenticated-proxy-re p)
          unauth-proxy-match (re-matches unauthenticated-proxy-re p)
          match->proxy-info (fn [m]
                              {:host (nth m 1)
                               :port (nth m 2)})]
      (cond
        auth-proxy-match
        (binding [*out* *err*]
          (println "WARNING: Proxy info is of authenticated type - discarding the user/pw as we do not support it!")
          (match->proxy-info auth-proxy-match))

        unauth-proxy-match
        (match->proxy-info unauth-proxy-match)

        :else
        (binding [*out* *err*]
          (println "WARNING: Can't parse proxy info - found:" s "- proceeding without using proxy!")
          nil)))))

(defn jvm-proxy-settings
  []
  (let [http-proxy  (parse-proxy-info (or (System/getenv "http_proxy")
                                          (System/getenv "HTTP_PROXY")))
        https-proxy (parse-proxy-info (or (System/getenv "https_proxy")
                                          (System/getenv "HTTPS_PROXY")))]
    (cond-> []
      http-proxy (concat [(format "-Dhttp.proxyHost=%s" (:host http-proxy))
                          (format "-Dhttp.proxyPort=%s" (:port http-proxy))])
      https-proxy (concat [(format "-Dhttps.proxyHost=%s" (:host https-proxy))
                           (format "-Dhttps.proxyPort=%s" (:port https-proxy))]))))

(defn -main [& command-line-args]
  (let [windows? (windows?)
        args (loop [command-line-args (seq command-line-args)
                    acc {}]
               (if command-line-args
                 (let [arg (first command-line-args)
                       bool-opt-keyword (get bool-opts->keyword arg)
                       string-opt-keyword (get string-opts->keyword arg)]
                   (cond
                     (= "--" arg) (assoc acc :args (next command-line-args))
                     (some #(str/starts-with? arg %) ["-J" "-R" "-C" "-O" "-M" "-A"])
                     (recur (next command-line-args)
                            (update acc (get parse-opts->keyword (subs arg 0 2))
                                    str (subs arg 2)))
                     bool-opt-keyword (recur
                                       (next command-line-args)
                                       (assoc acc bool-opt-keyword true))
                     string-opt-keyword (recur
                                         (nnext command-line-args)
                                         (assoc acc string-opt-keyword
                                                (second command-line-args)))
                     (str/starts-with? arg "-S") (binding [*out* *err*]
                                                   (println "Invalid option:" arg)
                                                   (System/exit 1))
                     (and
                      (not (some acc [:main-aliases :all-aliases]))
                      (or (= "-h" arg)
                          (= "--help" arg))) (assoc acc :help true)
                     :else (assoc acc :args command-line-args)))
                 acc))
        _ (when (:help args)
            (println help-text)
            (System/exit 0))
        java-cmd
        (let [java-cmd (which (if windows? "java.exe" "java"))]
          (if (str/blank? java-cmd)
            (let [java-home (System/getenv "JAVA_HOME")]
              (if-not (str/blank? java-home)
                (let [f (io/file java-home "bin" "java")]
                  (if (and (.exists f)
                           (.canExecute f))
                    (.getCanonicalPath f)
                    (throw (Exception. "Couldn't find 'java'. Please set JAVA_HOME."))))
                (throw (Exception. "Couldn't find 'java'. Please set JAVA_HOME."))))
            java-cmd))
        clojure-file (-> (which "clojure") (io/file))
        install-dir (when clojure-file
                      (with-open [reader (io/reader clojure-file)]
                        (let [lines (line-seq reader)]
                          (second (some #(re-matches #"^install_dir=(.*)" %) lines)))))
        downloaded-jar (io/file (home-dir)
                                ".deps.clj"
                                "ClojureTools"
                                (format "clojure-tools-%s.jar" version))
        tools-cp
        (or
         (System/getenv "CLOJURE_TOOLS_CP")
         (when (.exists downloaded-jar) (.getPath downloaded-jar))
         (binding [*out* *err*]
           (println (format "Could not find clojure-tools-%s.jar. Attempting download."
                            version))
           (clojure-tools-jar-download (io/file (home-dir) ".deps.clj"))
           downloaded-jar))
        deps-edn
        (or (:deps-file args)
            "deps.edn")
        clj-main-cmd
        (vec (concat [java-cmd]
                     (jvm-proxy-settings)
                     ["-Xms256m" "-classpath" tools-cp "clojure.main"]))]
    (when (:resolve-tags args)
      (let [f (io/file deps-edn)]
        (if (.exists f)
          (do (shell-command (into clj-main-cmd
                                   ["-m" "clojure.tools.deps.alpha.script.resolve-tags"
                                    (str "--deps-file=" deps-edn)]))
              (System/exit 0))
          (binding [*out* *err*]
            (println deps-edn "does not exist")
            (System/exit 1)))))
    (let [config-dir
          (or (System/getenv "CLJ_CONFIG")
              (when-let [xdg-config-home (System/getenv "XDG_CONFIG_HOME")]
                (.getPath (io/file xdg-config-home "clojure")))
              (.getPath (io/file (home-dir) ".clojure")))]
      ;; If user config directory does not exist, create it
      (let [config-dir (io/file config-dir)]
        (when-not (.exists config-dir)
          (.mkdirs config-dir)))
      (let [config-deps-edn (io/file config-dir "deps.edn")
            example-deps-edn (io/file install-dir "example-deps.edn")]
        (when (and install-dir
                   (not (.exists config-deps-edn))
                   (.exists example-deps-edn))
          (io/copy example-deps-edn config-deps-edn)))
      ;; Determine user cache directory
      (let [user-cache-dir
            (or (System/getenv "CLJ_CACHE")
                (when-let [xdg-config-home (System/getenv "XDG_CACHE_HOME")]
                  (.getPath (io/file xdg-config-home "clojure")))
                (.getPath (io/file config-dir ".cpcache")))
            ;; Chain deps.edn in config paths. repro=skip config dir
            config-user
            (when-not (:repro args)
              (.getPath (io/file config-dir "deps.edn")))
            config-project deps-edn
            config-paths
            (if (:repro args)
              (if install-dir [(.getPath (io/file install-dir "deps.edn")) deps-edn]
                  [])
              (if install-dir
                [(.getPath (io/file install-dir "deps.edn"))
                 (.getPath (io/file config-dir "deps.edn"))
                 deps-edn]
                [(.getPath (io/file config-dir "deps.edn"))
                 deps-edn]))
            ;; Determine whether to use user or project cache
            cache-dir
            (if (.exists (io/file "deps.edn"))
              ".cpcache"
              user-cache-dir)
            ;; Construct location of cached classpath file
            val*
            (str/join "|"
                      (concat [(:resolve-aliases args)
                               (:classpath-aliases args)
                               (:all-aliases args)
                               (:jvm-aliases args)
                               (:main-aliases args)
                               (:deps-data args)]
                              (map (fn [config-path]
                                     (if (.exists (io/file config-path))
                                       config-path
                                       "NIL"))
                                   config-paths)))
            ck (cksum val*)
            libs-file (.getPath (io/file cache-dir (str ck ".libs")))
            cp-file (.getPath (io/file cache-dir (str ck ".cp")))
            jvm-file (.getPath (io/file cache-dir (str ck ".jvm")))
            main-file (.getPath (io/file cache-dir (str ck ".main")))
            _ (when (:verbose args)
                (println "deps.clj version =" deps-clj-version)
                (println "version          =" version)
                (when install-dir (println "install_dir      =" install-dir))
                (println "config_dir       =" config-dir)
                (println "config_paths     =" (str/join " " config-paths))
                (println "cache_dir        =" cache-dir)
                (println "cp_file          =" cp-file)
                (println))
            stale
            (or (:force args)
                (:trace args)
                (not (.exists (io/file cp-file)))
                (let [cp-file (io/file cp-file)]
                  (some (fn [config-path]
                          (let [f (io/file config-path)]
                            (when (.exists f)
                              (> (.lastModified f)
                                 (.lastModified cp-file))))) config-paths)))
            tools-args
            (when (or stale (:pom args))
              (cond-> []
                (not (str/blank? (:deps-data args)))
                (conj "--config-data" (if windows?
                                        (pr-str (:deps-data args))
                                        (:deps-data args)))
                (:resolve-aliases args)
                (conj (str "-R" (:resolve-aliases args)))
                (:classpath-aliases args)
                (conj (str "-C" (:classpath-aliases args)))
                (:jvm-aliases args)
                (conj (str "-J" (:jvm-aliases args)))
                (:main-aliases args)
                (conj (str "-M" (:main-aliases args)))
                (:all-aliases args)
                (conj (str "-A" (:all-aliases args)))
                (:force-cp args)
                (conj "--skip-cp")
                (:threads args)
                (conj "--threads" (:threads args))
                (:trace args)
                (conj "--trace")))
            ;;  If stale, run make-classpath to refresh cached classpath
            _ (when (and stale (not (:describe args)))
                (when (:verbose args)
                  (println "Refreshing classpath"))
                (shell-command (into clj-main-cmd
                                     (concat
                                      ["-m" "clojure.tools.deps.alpha.script.make-classpath2"
                                       "--config-user" config-user
                                       "--config-project" config-project
                                       "--libs-file" (double-quote libs-file)
                                       "--cp-file" (double-quote cp-file)
                                       "--jvm-file" (double-quote jvm-file)
                                       "--main-file" (double-quote main-file)]
                                      tools-args))))
            cp
            (cond (:describe args) nil
                  (not (str/blank? (:force-cp args))) (:force-cp args)
                  :else (slurp (io/file cp-file)))]
        (cond (:pom args)
              (shell-command (into clj-main-cmd
                                   ["-m" "clojure.tools.deps.alpha.script.generate-manifest2"
                                    "--config-user" config-user
                                    "--config-project" config-project
                                    "--gen=pom" (str/join " " tools-args)]))
              (:print-classpath args)
              (println cp)
              (:describe args)
              (describe [[:deps-clj-version deps-clj-version]
                         [:version version]
                         [:config-files (filterv #(.exists (io/file %)) config-paths)]
                         [:config-user config-user]
                         [:config-project config-project]
                         (when install-dir [:install-dir install-dir])
                         [:cache-dir cache-dir]
                         [:force (boolean (:force args))]
                         [:repro (boolean (:repro args))]
                         [:resolve-aliases (str (:resolve-aliases args))]
                         [:classpath-aliases (str (:claspath-aliases args))]
                         [:jvm-aliases (str (:jvm-aliases args))]
                         [:main-aliases (str (:main-aliases args))]
                         [:all-aliases (str (:all-aliases args))]])
              (:tree args)
              (println (str/trim (shell-command (into clj-main-cmd
                                                      ["-m" "clojure.tools.deps.alpha.script.print-tree"
                                                       "--libs-file" libs-file])
                                                {:to-string? true})))
              (:trace args)
              (println "Writing trace.edn")
              (:command args)
              (let [command (str/replace (:command args) "{{classpath}}" (str cp))
                    main-cache-opts (when (.exists (io/file main-file))
                                      (slurp main-file))
                    command (str/replace command "{{main-opts}}" (str main-cache-opts))
                    command (str/split command #"\s+")
                    command (into command (:args args))]
                (shell-command command))
              :else
              (let [jvm-cache-opts (when (.exists (io/file jvm-file))
                                     (slurp jvm-file))
                    main-cache-opts (when (.exists (io/file main-file))
                                      (slurp main-file))
                    main-cache-opts (when main-cache-opts (str/split main-cache-opts #"\s"))
                    main-args (concat [java-cmd]
                                      (jvm-proxy-settings)
                                      [jvm-cache-opts
                                       (:jvm-opts args)
                                       (str "-Dclojure.libfile=" libs-file)
                                       "-classpath" cp
                                       "clojure.main"]
                                      main-cache-opts)
                    main-args (filterv some? main-args)
                    main-args (into main-args (:args args))]
                (shell-command main-args)))))))

(apply -main *command-line-args*)
