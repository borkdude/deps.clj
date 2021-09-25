(ns borkdude.deps
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import [java.lang ProcessBuilder$Redirect]
           [java.net URL HttpURLConnection]
           [java.nio.file Files FileSystems Path CopyOption])
  (:gen-class))

(set! *warn-on-reflection* true)
(def path-separator (System/getProperty "path.separator"))

;; see https://github.com/clojure/brew-install/blob/1.10.3/CHANGELOG.md
(def version "1.10.3.986")
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

(def windows?
  (-> (System/getProperty "os.name")
      (str/lower-case)
      (str/includes? "windows")))

(def ^:private ^:dynamic *dir* nil)

(def ^:private ^:dynamic *env* nil)
(def ^:private ^:dynamic *extra-env* nil)

(defn- as-string-map
  "Helper to coerce a Clojure map with keyword keys into something coerceable to Map<String,String>
  Stringifies keyword keys, but otherwise doesn't try to do anything clever with values"
  [m]
  (if (map? m)
    (into {} (map (fn [[k v]] [(str (if (keyword? k) (name k) k)) (str v)])) m)
    m))

(defn- add-env
  "Adds environment for a ProcessBuilder instance.
  Returns instance to participate in the thread-first macro."
  ^java.lang.ProcessBuilder [^java.lang.ProcessBuilder pb env]
  (doto (.environment pb)
    (.putAll (as-string-map env)))
  pb)

(defn- set-env
  "Sets environment for a ProcessBuilder instance.
  Returns instance to participate in the thread-first macro."
  ^java.lang.ProcessBuilder [^java.lang.ProcessBuilder pb env]
  (doto (.environment pb)
    (.clear)
    (.putAll (as-string-map env)))
  pb)

(defn shell-command
  "Executes shell command.

  Accepts the following options:

  `:to-string?`: instead of writing to stdoud, write to a string and
  return it."
  ([args] (shell-command args nil))
  ([args {:keys [:to-string?]}]
   (let [args (mapv str args)
         args (if (and windows? (not (System/getenv "DEPS_CLJ_NO_WINDOWS_FIXES")))
                (mapv #(str/replace % "\"" "\\\"") args)
                args)
         pb (cond-> (ProcessBuilder. ^java.util.List args)
              true (.redirectError ProcessBuilder$Redirect/INHERIT)
              (not to-string?) (.redirectOutput ProcessBuilder$Redirect/INHERIT)
              true (.redirectInput ProcessBuilder$Redirect/INHERIT))
         _ (when-let [dir *dir*]
             (.directory pb (io/file dir)))
         _ (when-let [env *env*]
             (set-env pb env))
         _ (when-let [extra-env *extra-env*]
             (add-env pb extra-env))
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
  Start a REPL  clj     [clj-opt*] [-Aaliases] [init-opt*]
  Exec fn(s)    clojure [clj-opt*] -X[aliases] [a/fn*] [kpath v]*
  Run main      clojure [clj-opt*] -M[aliases] [init-opt*] [main-opt] [arg*]
  Run tool      clojure [clj-opt*] -T[name|aliases] a/fn [kpath v] kv-map?
  Prepare       clojure [clj-opt*] -P [other exec opts]

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
 -X:deps find-versions     Find available versions of a library
 -X:deps prep              Prepare all unprepped libs in the dep tree

For more info, see:
 https://clojure.org/guides/deps_and_cli
 https://clojure.org/reference/repl_and_main"))

(defn describe-line [[kw val]]
  (pr kw val))

(defn describe [lines]
  (let [[first-line & lines] lines]
    (print "{") (describe-line first-line)
    (doseq [line lines
            :when line]
      (print "\n ") (describe-line line))
    (println "}")))

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
  (when-let [path (System/getenv "PATH")]
    (let [paths (.split path path-separator)]
      (loop [paths paths]
        (when-first [p paths]
          (let [f (io/file p executable)]
            (if (and (.isFile f)
                     (.canExecute f))
              (.getCanonicalPath f)
              (recur (rest paths)))))))))

(defn home-dir []
  (if windows?
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

(def vconj (fnil conj []))

(defn parse-args [args]
  (loop [args (seq args)
         acc {:mode :repl}]
    (if args
      (let [arg (first args)
            [arg args]
            ;; workaround for Powershell, see GH-42
            (if (and windows? (#{"-X:" "-M:" "-A:" "-T:"} arg))
              [(str arg (second args))
               (next args)]
              [arg args])
            bool-opt-keyword (get bool-opts->keyword arg)
            string-opt-keyword (get string-opts->keyword arg)]
        (cond
          (= "--" arg) (assoc acc :args (next args))
          (or (= "-version" arg)
              (= "--version" arg)) (assoc acc :version true)
          (str/starts-with? arg "-M")
          (assoc acc
                 :mode :main
                 :main-aliases (non-blank (subs arg 2))
                 :args (next args))
          (str/starts-with? arg "-X")
          (assoc acc
                 :mode :exec
                 :exec-aliases (non-blank (subs arg 2))
                 :args (next args))
          (str/starts-with? arg "-T:")
          (assoc acc
                 :mode :tool
                 :tool-aliases (non-blank (subs arg 2))
                 :args (next args))
          (str/starts-with? arg "-T")
          (assoc acc
                 :mode :tool
                 :tool-name (non-blank (subs arg 2))
                 :args (next args))
          ;; deprecations
          (some #(str/starts-with? arg %) ["-R" "-C"])
          (do (warn arg "is deprecated, use -A with repl, -M for main, or -X for exec")
              (recur (next args)
                     (update acc (get parse-opts->keyword (subs arg 0 2))
                             vconj (subs arg 2))))
          (some #(str/starts-with? arg %) ["-O"])
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
          (recur (next args)
                 (update acc (get parse-opts->keyword (subs arg 0 2))
                         vconj (non-blank (subs arg 2))))
          bool-opt-keyword (recur
                            (next args)
                            (assoc acc bool-opt-keyword true))
          string-opt-keyword (recur
                              (nnext args)
                              (assoc acc string-opt-keyword
                                     (second args)))
          (str/starts-with? arg "-S") (let [msg (str "Invalid option: " arg)]
                                        (*exit-fn* 1 msg))
          (and
           (not (some acc [:main-aliases :all-aliases]))
           (or (= "-h" arg)
               (= "--help" arg))) (assoc acc :help true)
          :else (assoc acc :args args)))
      acc)))


(defn- ^Path as-path
  [path]
  (if (instance? Path path) path
      (.toPath (io/file path))))

(defn ^Path relativize
  "Returns relative path by comparing this with other."
  [f]
  ;; (prn :dir *dir* :f f)
  (if-let [dir *dir*]
    (.relativize (as-path dir) (as-path f))
    f))

(defn -main [& command-line-args]
  (let [opts (parse-args command-line-args)
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
        mode (:mode opts)
        exec? (= :exec mode)
        tool? (= :tool mode)
        exec-cp (when (or exec? tool?)
                  (.getPath exec-jar))
        deps-edn
        (or (:deps-file opts)
            (.getPath (io/file *dir* "deps.edn")))
        clj-main-cmd
        (vec (concat [java-cmd]
                     proxy-settings
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
    (let [config-tools-edn (io/file config-dir "tools.edn")
          example-tools-edn (io/file install-dir "example-tools.edn")]
      (when (and install-dir
                 (not (.exists config-tools-edn))
                 (.exists example-tools-edn))
        (io/copy example-tools-edn config-tools-edn)))
    ;; Determine user cache directory
    (let [user-cache-dir
          (or (System/getenv "CLJ_CACHE")
              (when-let [xdg-config-home (System/getenv "XDG_CACHE_HOME")]
                (.getPath (io/file xdg-config-home "clojure")))
              (.getPath (io/file config-dir ".cpcache")))
          ;; Chain deps.edn in config paths. repro=skip config dir
          config-user
          (when-not (:repro opts)
            (.getPath (io/file config-dir "deps.edn")))
          config-project deps-edn
          config-paths
          (if (:repro opts)
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
          (if (.exists (io/file deps-edn))
            (.getPath (io/file *dir* ".cpcache"))
            user-cache-dir)
          ;; Construct location of cached classpath file
          tool-name (:tool-name opts)
          tool-aliases (:tool-aliases opts)
          val*
          (str/join "|"
                    (concat (:resolve-aliases opts)
                            (:classpath-aliases opts)
                            (:repl-aliases opts)
                            [(:exec-aliases opts)
                             (:main-aliases opts)
                             (:deps-data opts)
                             tool-name
                             (:tool-aliases opts)]
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
          _ (when (:verbose opts)
              (println "deps.clj version =" deps-clj-version)
              (println "version          =" version)
              (when install-dir (println "install_dir      =" install-dir))
              (println "config_dir       =" config-dir)
              (println "config_paths     =" (str/join " " config-paths))
              (println "cache_dir        =" cache-dir)
              (println "cp_file          =" cp-file)
              (println))
          tree? (:tree opts)
          stale
          (or (:force opts)
              (:trace opts)
              tree?
              (:prep opts)
              (not (.exists (io/file cp-file)))
              (when tool-name
                (let [tool-file (io/file config-dir "tools" (str tool-name ".edn"))]
                  (when (.exists tool-file)
                    (> (.lastModified tool-file)
                       (.lastModified (io/file cp-file))))))
              (let [cp-file (io/file cp-file)]
                (some (fn [config-path]
                        (let [f (io/file config-path)]
                          (when (.exists f)
                            (> (.lastModified f)
                               (.lastModified cp-file))))) config-paths)))
          tools-args
          (when (or stale (:pom opts))
            (cond-> []
              (not (str/blank? (:deps-data opts)))
              (conj "--config-data" (:deps-data opts))
              (:resolve-aliases opts)
              (conj (str "-R" (str/join "" (:resolve-aliases opts))))
              (:classpath-aliases opts)
              (conj (str "-C" (str/join "" (:classpath-aliases opts))))
              (:main-aliases opts)
              (conj (str "-M" (:main-aliases opts)))
              (:repl-aliases opts)
              (conj (str "-A" (str/join "" (:repl-aliases opts))))
              (:exec-aliases opts)
              (conj (str "-X" (:exec-aliases opts)))
              tool?
              (conj "--tool-mode")
              tool-name
              (conj "--tool-name" tool-name)
              tool-aliases
              (conj (str "-T" tool-aliases))
              (:force-cp opts)
              (conj "--skip-cp")
              (:threads opts)
              (conj "--threads" (:threads opts))
              (:trace opts)
              (conj "--trace")
              tree?
              (conj "--tree")))]
      ;;  If stale, run make-classpath to refresh cached classpath
      (when (and stale (not (or (:describe opts)
                                (:help opts)
                                (:version opts))))
        (when (:verbose opts)
          (warn "Refreshing classpath"))
        (let [res (shell-command (into clj-main-cmd
                                      (concat
                                       ["-m" "clojure.tools.deps.alpha.script.make-classpath2"
                                        "--config-user" config-user
                                        "--config-project" (relativize config-project)
                                        "--basis-file" (relativize basis-file)
                                        "--libs-file" (relativize libs-file)
                                        "--cp-file" (relativize cp-file)
                                        "--jvm-file" (relativize jvm-file)
                                        "--main-file" (relativize main-file)]
                                       tools-args))
                                 {:to-string? tree?})]
          (when tree?
            (print res) (flush))))
      (let [cp (cond (or (:describe opts)
                         (:prep opts)
                         (:help opts)) nil
                     (not (str/blank? (:force-cp opts))) (:force-cp opts)
                     :else (slurp (io/file cp-file)))]
        (cond (:help opts) (do (println help-text)
                               (*exit-fn* 0))
              (:version opts) (do (println "Clojure CLI version (deps.clj)" version)
                                  (*exit-fn* 0))
              (:prep opts) (*exit-fn* 0)
              (:pom opts)
              (shell-command (into clj-main-cmd
                                   ["-m" "clojure.tools.deps.alpha.script.generate-manifest2"
                                    "--config-user" config-user
                                    "--config-project" (relativize config-project)
                                    "--gen=pom" (str/join " " tools-args)]))
              (:print-classpath opts)
              (println cp)
              (:describe opts)
              (describe [[:deps-clj-version deps-clj-version]
                         [:version version]
                         [:config-files (filterv #(.exists (io/file %)) config-paths)]
                         [:config-user config-user]
                         [:config-project (relativize config-project)]
                         (when install-dir [:install-dir install-dir])
                         [:config-dir config-dir]
                         [:cache-dir cache-dir]
                         [:force (boolean (:force opts))]
                         [:repro (boolean (:repro opts))]
                         [:main-aliases (str (:main-aliases opts))]
                         [:all-aliases (str (:all-aliases opts))]])
              tree? (*exit-fn* 0)
              (:trace opts)
              (warn "Wrote trace.edn")
              (:command opts)
              (let [command (str/replace (:command opts) "{{classpath}}" (str cp))
                    main-cache-opts (when (.exists (io/file main-file))
                                      (-> main-file slurp str/split-lines))
                    main-cache-opts (str/join " " main-cache-opts)
                    command (str/replace command "{{main-opts}}" (str main-cache-opts))
                    command (str/split command #"\s+")
                    command (into command (:args opts))]
                (*process-fn* command))
              :else
              (let [jvm-cache-opts (when (.exists (io/file jvm-file))
                                     (-> jvm-file slurp str/split-lines))
                    main-cache-opts (when (.exists (io/file main-file))
                                      (-> main-file slurp str/split-lines))
                    main-opts (if (or exec? tool?)
                                ["-m" "clojure.run.exec"]
                                main-cache-opts)
                    cp (if (or exec? tool?)
                         (str cp path-separator exec-cp)
                         cp)
                    main-args (concat [java-cmd]
                                      proxy-settings
                                      jvm-cache-opts
                                      (:jvm-opts opts)
                                      [(str "-Dclojure.basis=" (relativize basis-file))
                                       (str "-Dclojure.libfile=" (relativize libs-file))
                                       "-classpath" cp
                                       "clojure.main"]
                                      main-opts)
                    main-args (filterv some? main-args)
                    main-args (into main-args (:args opts))]
                (when (and (= :repl mode)
                           (pos? (count (:args opts))))
                  (warn "WARNING: Implicit use of clojure.main with options is deprecated, use -M"))
                (*process-fn* main-args)))))))
