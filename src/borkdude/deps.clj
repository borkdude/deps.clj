(ns borkdude.deps
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import [java.lang ProcessBuilder$Redirect]
           [java.net URL HttpURLConnection]
           [java.nio.file Files FileSystems CopyOption])
  (:gen-class))

(set! *warn-on-reflection* true)
(def path-separator (System/getProperty "path.separator"))

(def version "1.10.3.822")
(def deps-clj-version
  (-> (io/resource "DEPS_CLJ_VERSION")
      (slurp)
      (str/trim)))

(defn warn [& strs]
  (binding [*out* *err*]
    (apply println strs)))

(def ^:private ^:dynamic *exit-fn*
  (fn
    ([exit-code] (System/exit exit-code))
    ([exit-code msg]
     (warn msg)
     (System/exit exit-code))))

(defn shell-command
  "Executes shell command.

  Accepts the following options:

  `:to-string?`: instead of writing to stdoud, write to a string and
  return it."
  ([args] (shell-command args nil))
  ([args {:keys [:to-string?]}]
   (let [args (mapv str args)
         pb (cond-> (ProcessBuilder. ^java.util.List args)
              true (.redirectError ProcessBuilder$Redirect/INHERIT)
              (not to-string?) (.redirectOutput ProcessBuilder$Redirect/INHERIT)
              true (.redirectInput ProcessBuilder$Redirect/INHERIT))
         proc (.start pb)
         string-out
         (when to-string?
           (let [sw (java.io.StringWriter.)]
             (with-open [w (io/reader (.getInputStream proc))]
               (io/copy w sw))
             (str sw)))
         exit-code (.waitFor proc)]
     (when (not (zero? exit-code))
       (*exit-fn* exit-code))
     string-out)))

(def ^:private ^:dynamic *process-fn* shell-command)

(def help-text (str "Version: " version "

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
  Start a REPL   clj     [clj-opt*] [-Aaliases] [init-opt*]
  Exec function  clojure [clj-opt*] -X[aliases] [a/fn] [kpath v]*
  Run main       clojure [clj-opt*] -M[aliases] [init-opt*] [main-opt] [arg*]
  Prepare        clojure [clj-opt*] -P [other exec opts]

exec-opts:
 -Aaliases      Use concatenated aliases to modify classpath
 -X[aliases]    Use concatenated aliases to modify classpath or supply exec fn/args
 -M[aliases]    Use concatenated aliases to modify classpath or supply main opts
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
 --version      Print the version to stdout and exit
 -version       Print the version to stdout and exit

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

(def win? (delay (windows?)))

(defn double-quote
  "Double quotes shell arguments on Windows. On other platforms it just
  passes through the string."
  [s]
  (if @win?
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
        paths (.split path path-separator)]
    (loop [paths paths]
      (when-first [p paths]
        (let [f (io/file p executable)]
          (if (and (.isFile f)
                   (.canExecute f))
            (.getCanonicalPath f)
            (recur (rest paths))))))))

(defn home-dir []
  (if @win?
    ;; workaround for https://github.com/oracle/graal/issues/1630
    (System/getenv "userprofile")
    (System/getProperty "user.home")))

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

(defn proxy-info [m]
  {:host (nth m 1)
   :port (nth m 2)})

(defn parse-proxy-info
  [s]
  (when s
    (let [p (cond
              (clojure.string/starts-with? s "http://") (subs s 7)
              (clojure.string/starts-with? s "https://") (subs s 8)
              :else s)
          auth-proxy-match (re-matches authenticated-proxy-re p)
          unauth-proxy-match (re-matches unauthenticated-proxy-re p)]
      (cond
        auth-proxy-match
        (do (warn "WARNING: Proxy info is of authenticated type - discarding the user/pw as we do not support it!")
            (proxy-info auth-proxy-match))

        unauth-proxy-match
        (proxy-info unauth-proxy-match)

        :else
        (do (warn "WARNING: Can't parse proxy info - found:" s "- proceeding without using proxy!")
            nil)))))

(defn jvm-proxy-settings
  []
  (let [http-proxy  (parse-proxy-info (or (System/getenv "http_proxy")
                                          (System/getenv "HTTP_PROXY")))
        https-proxy (parse-proxy-info (or (System/getenv "https_proxy")
                                          (System/getenv "HTTPS_PROXY")))]
    (when http-proxy
      (System/setProperty "http.proxyHost" (:host http-proxy))
      (System/setProperty "http.proxyPort" (:port http-proxy)))
    (when https-proxy
      (System/setProperty "https.proxyHost" (:host https-proxy))
      (System/setProperty "https.proxyPort" (:port https-proxy)))
    (cond-> []
      http-proxy (concat  [(str "-Dhttp.proxyHost=" (:host http-proxy))
                           (str "-Dhttp.proxyPort=" (:port http-proxy))])
      https-proxy (concat [(str "-Dhttps.proxyHost=" (:host https-proxy))
                           (str "-Dhttps.proxyPort=" (:port https-proxy))]))))

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
  (let [windows? @win?
        args (loop [command-line-args (seq command-line-args)
                    acc {:mode :repl}]
               (if command-line-args
                 (let [arg (first command-line-args)
                       [arg command-line-args]
                       ;; workaround for Powershell, see GH-42
                       (if (and windows? (#{"-X:" "-M:" "-A:"} arg))
                         [(str arg (second command-line-args))
                          (next command-line-args)]
                         [arg command-line-args])
                       bool-opt-keyword (get bool-opts->keyword arg)
                       string-opt-keyword (get string-opts->keyword arg)]
                   (cond
                     (= "--" arg) (assoc acc :args (next command-line-args))
                     (or (= "-version" arg)
                         (= "--version" arg)) (assoc acc :version true)
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
                     (let [msg (str arg " is no longer supported, use -A with repl, -M for main, or -X for exec")]
                       (*exit-fn* 1 msg))
                     (= "-Sresolve-tags" arg)
                     (let [msg "Option changed, use: clj -X:deps git-resolve-tags"]
                       (*exit-fn* 1 msg))
                     ;; end deprecations
                     (= "-A" arg)
                     (let [msg "-A requires an alias"]
                       (*exit-fn* 1 msg))
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
                     (str/starts-with? arg "-S") (let [msg (str "Invalid option: " arg)]
                                                   (*exit-fn* 1 msg))
                     (and
                      (not (some acc [:main-aliases :all-aliases]))
                      (or (= "-h" arg)
                          (= "--help" arg))) (assoc acc :help true)
                     :else (assoc acc :args command-line-args)))
                 acc))
        java-cmd
        (or (System/getenv "JAVA_CMD")
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
                java-cmd)))
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
        proxy-settings (jvm-proxy-settings) ;; side effecting, sets java proxy properties for download
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
                     proxy-settings
                     ["-Xms256m" "-classpath" (double-quote tools-cp) "clojure.main"]))
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
          tree? (:tree args)
          stale
          (or (:force args)
              (:trace args)
              tree?
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
              (:exec-aliases args)
              (conj (str "-X" (:exec-aliases args)))
              (:force-cp args)
              (conj "--skip-cp")
              (:threads args)
              (conj "--threads" (:threads args))
              (:trace args)
              (conj "--trace")
              tree?
              (conj "--tree")))]
      ;;  If stale, run make-classpath to refresh cached classpath
      (when (and stale (not (or (:describe args)
                                (:help args)
                                (:version args))))
        (when (:verbose args)
          (warn "Refreshing classpath"))
        (let [res (shell-command (into clj-main-cmd
                                      (concat
                                       ["-m" "clojure.tools.deps.alpha.script.make-classpath2"
                                        "--config-user" (double-quote config-user)
                                        "--config-project" (double-quote config-project)
                                        "--basis-file" (double-quote basis-file)
                                        "--libs-file" (double-quote libs-file)
                                        "--cp-file" (double-quote cp-file)
                                        "--jvm-file" (double-quote jvm-file)
                                        "--main-file" (double-quote main-file)]
                                       tools-args))
                                 {:to-string? tree?})]
          (when tree?
            (print res) (flush))))
      (let [cp (cond (or (:describe args)
                         (:prep args)
                         (:help nil)) nil
                     (not (str/blank? (:force-cp args))) (:force-cp args)
                     :else (slurp (io/file cp-file)))]
        (cond (:help args) (do (println help-text)
                               (*exit-fn* 0))
              (:version args) (do (println "Clojure CLI version (deps.clj)" version)
                                  (*exit-fn* 0))
              (:prep args) (*exit-fn* 0)
              (:pom args)
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
              tree? (*exit-fn* 0)
              (:trace args)
              (warn "Wrote trace.edn")
              (:command args)
              (let [command (str/replace (:command args) "{{classpath}}" (str cp))
                    main-cache-opts (when (.exists (io/file main-file))
                                      (-> main-file slurp str/split-lines))
                    main-cache-opts (str/join " " main-cache-opts)
                    command (str/replace command "{{main-opts}}" (str main-cache-opts))
                    command (str/split command #"\s+")
                    command (into command (:args args))]
                (*process-fn* command))
              :else
              (let [exec-args (when-let [aliases (:exec-aliases args)]
                                ["--aliases" aliases])
                    jvm-cache-opts (when (.exists (io/file jvm-file))
                                     (-> jvm-file slurp str/split-lines))
                    main-cache-opts (when (.exists (io/file main-file))
                                      (-> main-file slurp str/split-lines))
                    main-opts (if exec?
                                (into ["-m" "clojure.run.exec"]
                                      exec-args)
                                main-cache-opts)
                    cp (if exec?
                         (str cp path-separator exec-cp)
                         cp)
                    main-args (concat [java-cmd]
                                      proxy-settings
                                      jvm-cache-opts
                                      (:jvm-opts args)
                                      [(str "-Dclojure.basis=" basis-file)
                                       (str "-Dclojure.libfile=" libs-file)
                                       "-classpath" cp
                                       "clojure.main"]
                                      main-opts)
                    main-args (filterv some? main-args)
                    main-args (into main-args (:args args))]
                (*process-fn* main-args)))))))
