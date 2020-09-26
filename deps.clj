#!/usr/bin/env bb

;; Generated with script/gen_script.clj. Do not edit directly.

(ns borkdude.deps
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import [java.lang ProcessBuilder$Redirect]
           [java.net URL HttpURLConnection]
           [java.nio.file Files FileSystems CopyOption]))

(set! *warn-on-reflection* true)

(def version "1.10.1.697")
(def deps-clj-version "0.0.10-SNAPSHOT")

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
Version: 1.10.1.697

You use the Clojure tools ('clj' or 'clojure') to run Clojure programs
on the JVM, e.g. to start a REPL or invoke a specific function with data.
The Clojure tools will configure the JVM process by defining a classpath
(of desired libraries), an execution environment (JVM options) and
specifying a main class and args.

Using a deps.edn file (or files), you tell Clojure where your source code
resides and what libraries you need. Clojure will then calculate the full
set of required libraries and a classpath, caching expensive parts of this
process for better performance.

The internal steps of the Clojure tools, as well as the Clojure functions
you intend to run, are parameterized by data structures, often maps. Shell
command lines are not optimized for passing nested data, so instead you
will put the data structures in your deps.edn file and refer to them on the
command line via 'aliases' - keywords that name data structures.

'clj' and 'clojure' differ in that 'clj' has extra support for use as a REPL
in a terminal, and should be preferred unless you don't want that support,
then use 'clojure'.

Usage:
  Start a REPL   clj     [clj-opt*] [-A:aliases] [init-opt*]
  Exec function  clojure [clj-opt*] -X[:aliases] [a/fn] [kpath v]*
  Run main       clojure [clj-opt*] -M[:aliases] [init-opt*] [main-opt] [arg*]
  Prepare        clojure [clj-opt*] -P [other exec opts]

exec-opts:
 -A:aliases     Use aliases to modify classpath
 -X[:aliases]   Use aliases to modify classpath or supply exec fn/args
 -M[:aliases]   Use aliases to modify classpath or supply main opts
 -P             Prepare deps - download libs, cache classpath, but don't exec

clj-opts:
 -Jopt          Pass opt through in java_opts, ex: -J-Xmx512m
 -Sdeps EDN     Deps data to use as the last deps file to be merged
 -Spath         Compute classpath and echo to stdout only
 -Spom          Generate (or update) pom.xml with deps and paths
 -Stree         Print dependency tree
 -Scp CP        Do NOT compute or cache classpath, use this one instead
 -Srepro        Ignore the ~/.clojure/deps.edn config file
 -Sforce        Force recomputation of the classpath (don't use the cache)
 -Sverbose      Print important path info to console
 -Sdescribe     Print environment and command parsing info as data
 -Sthreads      Set specific number of download threads
 -Strace        Write a trace.edn file that traces deps expansion
 --             Stop parsing dep options and pass remaining arguments to clojure.main

The following non-standard options are available only in deps.clj:

 -Sdeps-file    Use this file instead of deps.edn
 -Scommand      A custom command that will be invoked. Substitutions: {{classpath}}, {{main-opts}}.

init-opt:
 -i, --init path     Load a file or resource
 -e, --eval string   Eval exprs in string; print non-nil values
 --report target     Report uncaught exception to \"file\" (default), \"stderr\", or \"none\"

main-opt:
 -m, --main ns-name  Call the -main function from namespace w/args
 -r, --repl          Run a repl
 path                Run a script from a file or resource
 -                   Run a script from standard input
 -h, -?, --help      Print this help message and exit

Programs provided by :deps alias:
 -X:deps mvn-install       Install a maven jar to the local repository cache
 -X:deps git-resolve-tags  Resolve git coord tags to shas and update deps.edn

For more info, see:
 https://clojure.org/guides/deps_and_cli
 https://clojure.org/reference/repl_and_main"))

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

(defn warn [& strs]
  (binding [*out* *err*]
    (apply println strs)))

(defn download [source dest]
  (warn "Attempting download from" source)
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
        _ (.mkdirs (io/file destination-dir))
        fs (FileSystems/newFileSystem (.toPath zip-file) nil)]
    (doseq [f [clojure-tools-jar "exec.jar" "example-deps.edn"]]
      (let [file-in-zip (.getPath fs "ClojureTools" (into-array String [f]))]
        (Files/copy file-in-zip (.toPath (io/file destination-dir f))
                    ^{:tag "[Ljava.nio.file.CopyOption;"}
                    (into-array CopyOption
                                [java.nio.file.StandardCopyOption/REPLACE_EXISTING]))))))

(defn clojure-tools-jar-download
  "Downloads clojure tools jar into deps-clj-config-dir."
  [deps-clj-config-dir]
  (let [dir (io/file deps-clj-config-dir)
        zip (io/file deps-clj-config-dir "tools.zip")]
    (.mkdirs dir)
    (download (format "https://download.clojure.org/install/clojure-tools-%s.zip" version)
              zip)
    (unzip zip (.getPath dir))
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

(def parse-opts->keyword
  {"-J" :jvm-opts
   "-R" :resolve-aliases
   "-C" :classpath-aliases
   "-M" :main-aliases
   "-A" :repl-aliases
   })

(def bool-opts->keyword
  {"-Spath" :print-classpath
   "-Sverbose" :verbose
   "-Strace" :trace
   "-Sdescribe" :describe
   "-Sforce" :force
   "-Srepro" :repro
   "-Stree" :tree
   "-Spom" :pom
   "-P" :prep})

(def string-opts->keyword
  {"-Sdeps" :deps-data
   "-Scp" :force-cp
   "-Sdeps-file" :deps-file
   "-Scommand" :command
   "-Sthreads" :threads})

(defn non-blank [s]
  (when-not (str/blank? s)
    s))

(defn -main [& command-line-args]
  (let [windows? (windows?)
        args (loop [command-line-args (seq command-line-args)
                    acc {:mode :repl}]
               (if command-line-args
                 (let [arg (first command-line-args)
                       bool-opt-keyword (get bool-opts->keyword arg)
                       string-opt-keyword (get string-opts->keyword arg)]
                   (cond
                     (= "--" arg) (assoc acc :args (next command-line-args))
                     (str/starts-with? arg "-M")
                     (assoc acc
                            :mode :main
                            :main-aliases (non-blank (subs arg 2))
                            :args (next command-line-args))
                     (str/starts-with? arg "-X")
                     (assoc acc
                            :mode :exec
                            :exec-aliases (non-blank (subs arg 2))
                            :args (next command-line-args))
                     ;; deprecations
                     (some #(str/starts-with? arg %) ["-R" "-C"])
                     (do (warn arg "is deprecated, use -A with repl, -M for main, or -X for exec")
                         (recur (next command-line-args)
                                (update acc (get parse-opts->keyword (subs arg 0 2))
                                        str (subs arg 2))))
                     (some #(str/starts-with? arg %) ["-O" "-T"])
                     (do (warn arg "is no longer supported, use -A with repl, -M for main, or -X for exec")
                         (System/exit 1))
                     (= "-Sresolve-tags" arg)
                     (do (warn "Option changed, use: clj -X:deps git-resolve-tags")
                         (System/exit 1))
                     ;; end deprecations
                     (some #(str/starts-with? arg %) ["-J" "-C" "-O" "-A"])
                     (recur (next command-line-args)
                            (update acc (get parse-opts->keyword (subs arg 0 2))
                                    str (non-blank (subs arg 2))))
                     bool-opt-keyword (recur
                                       (next command-line-args)
                                       (assoc acc bool-opt-keyword true))
                     string-opt-keyword (recur
                                         (nnext command-line-args)
                                         (assoc acc string-opt-keyword
                                                (second command-line-args)))
                     (str/starts-with? arg "-S") (do (warn "Invalid option:" arg)
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
        tools-dir (or (System/getenv "CLOJURE_TOOLS_DIR") ;; TODO document
                      (.getPath (io/file (home-dir)
                                         ".deps.clj"
                                         version
                                         "ClojureTools")))
        tools-jar (io/file tools-dir
                           (format "clojure-tools-%s.jar" version))
        exec-jar (io/file tools-dir "exec.jar")
        tools-cp
        (or
         (when (.exists tools-jar) (.getPath tools-jar))
         (binding [*out* *err*]
           (println (format "Could not find %s" tools-jar))
           (clojure-tools-jar-download tools-dir)
           tools-jar))
        exec? (= :exec (:mode args))
        exec-cp (when exec?
                  (.getPath exec-jar))
        deps-edn
        (or (:deps-file args)
            "deps.edn")
        clj-main-cmd
        (vec (concat [java-cmd]
                     (jvm-proxy-settings)
                     ["-Xms256m" "-classpath" tools-cp "clojure.main"]))
        config-dir
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
                             (:repl-aliases args)
                             (:exec-aliases args)
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
          basis-file (.getPath (io/file cache-dir (str ck ".basis")))
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
              (:prep args)
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
              (:main-aliases args)
              (conj (str "-M" (:main-aliases args)))
              (:repl-aliases args)
              (conj (str "-A" (:repl-aliases args)))
              exec?
              (conj (str "-X" (:exec-aliases args)))
              (:force-cp args)
              (conj "--skip-cp")
              (:threads args)
              (conj "--threads" (:threads args))
              (:trace args)
              (conj "--trace")))]
      ;;  If stale, run make-classpath to refresh cached classpath
      (when (and stale (not (:describe args)))
        (when (:verbose args)
          (warn "Refreshing classpath"))
        (shell-command (into clj-main-cmd
                             (concat
                              ["-m" "clojure.tools.deps.alpha.script.make-classpath2"
                               "--config-user" config-user
                               "--config-project" config-project
                               "--basis-file" basis-file
                               "--libs-file" (double-quote libs-file)
                               "--cp-file" (double-quote cp-file)
                               "--jvm-file" (double-quote jvm-file)
                               "--main-file" (double-quote main-file)]
                              tools-args))))
      (when (:prep args)
        (System/exit 0))
      (let [cp (cond (:describe args) nil
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
                         [:main-aliases (str (:main-aliases args))]
                         [:all-aliases (str (:all-aliases args))]])
              (:tree args)
              (println (str/trim (shell-command (into clj-main-cmd
                                                      ["-m" "clojure.tools.deps.alpha.script.print-tree"
                                                       "--libs-file" libs-file])
                                                {:to-string? true})))
              (:trace args)
              (warn "Wrote trace.edn")
              (:command args)
              (let [command (str/replace (:command args) "{{classpath}}" (str cp))
                    main-cache-opts (when (.exists (io/file main-file))
                                      (slurp main-file))
                    command (str/replace command "{{main-opts}}" (str main-cache-opts))
                    command (str/split command #"\s+")
                    command (into command (:args args))]
                (shell-command command))
              :else
              (let [exec-args (when-let [aliases (:exec-aliases args)]
                                ["--aliases" aliases])
                    jvm-cache-opts (when (.exists (io/file jvm-file))
                                     (slurp jvm-file))
                    main-args (if exec?
                                (into ["-m" "clojure.run.exec"]
                                      exec-args)
                                (some-> (when (.exists (io/file main-file))
                                          (slurp main-file))
                                        (str/split #"\s")))
                    cp (if exec?
                         (str cp ":" exec-cp)
                         cp)
                    main-args (concat [java-cmd]
                                      (jvm-proxy-settings)
                                      [jvm-cache-opts
                                       (:jvm-opts args)
                                       (str "-Dclojure.basis=" basis-file)
                                       (str "-Dclojure.libfile=" libs-file)
                                       "-classpath" cp
                                       "clojure.main"]
                                      main-args)
                    main-args (filterv some? main-args)
                    main-args (into main-args (:args args))]
                (shell-command main-args)))))))

(apply -main *command-line-args*)
