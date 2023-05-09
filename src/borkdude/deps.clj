(ns borkdude.deps
  "Port of https://github.com/clojure/brew-install/blob/1.11.1/src/main/resources/clojure/install/clojure in Clojure"
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str])
  (:import
   [java.lang ProcessBuilder$Redirect]
   [java.net HttpURLConnection URL URLConnection]
   [java.nio.file CopyOption Files Path]
   [java.util.zip ZipInputStream])
  (:gen-class))

(set! *warn-on-reflection* true)
(def path-separator (System/getProperty "path.separator"))

;; see https://github.com/clojure/brew-install/blob/1.11.1/CHANGELOG.md
(def version
  (delay (or (System/getenv "DEPS_CLJ_TOOLS_VERSION")
             "1.11.1.1273")))

(def cache-version "4")

(def deps-clj-version
  (-> (io/resource "DEPS_CLJ_VERSION")
      (slurp)
      (str/trim)))

(defn warn [& strs]
  (binding [*out* *err*]
    (apply println strs)))

#_{:clj-kondo/ignore [:unused-private-var]}
(defn- -debug [& strs]
  (.println System/err
            (with-out-str
              (apply println strs))))

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

(def help-text (delay (str "Version: " @version "

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
  Start a REPL  clj     [clj-opt*] [-Aaliases]
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
 -X:deps mvn-pom Generate (or update) pom.xml with deps and paths
 -X:deps list              List full transitive deps set and licenses
 -X:deps tree              Print deps tree
 -X:deps find-versions     Find available versions of a library
 -X:deps prep              Prepare all unprepped libs in the dep tree
 -X:deps mvn-install       Install a maven jar to the local repository cache
 -X:deps git-resolve-tags  Resolve git coord tags to shas and update deps.edn

For more info, see:
 https://clojure.org/guides/deps_and_cli
 https://clojure.org/reference/repl_and_main")))

(defn describe-line [[kw val]]
  (pr kw val))

(defn describe [lines]
  (let [[first-line & lines] lines]
    (print "{") (describe-line first-line)
    (doseq [line lines
            :when line]
      (print "\n ") (describe-line line))
    (println "}")))

(defn ^:private ^:dynamic *getenv-fn*
  "Get ENV'ironment variable."
  ^String [env]
  (java.lang.System/getenv env))

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
  (when-let [path (*getenv-fn* "PATH")]
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
    (*getenv-fn* "userprofile")
    (System/getProperty "user.home")))

(def java-exe (if windows? "java.exe" "java"))

(defn- get-java-cmd
  "Returns path to java executable to invoke sub commands with."
  []
  (or (*getenv-fn* "JAVA_CMD")
      (let [java-cmd (which java-exe)]
        (if (str/blank? java-cmd)
          (let [java-home (*getenv-fn* "JAVA_HOME")]
            (if-not (str/blank? java-home)
              (let [f (io/file java-home "bin" java-exe)]
                (if (and (.exists f)
                         (.canExecute f))
                  (.getCanonicalPath f)
                  (throw (Exception. "Couldn't find 'java'. Please set JAVA_HOME."))))
              (throw (Exception. "Couldn't find 'java'. Please set JAVA_HOME."))))
          java-cmd))))

(def ^:private authenticated-proxy-re #".+:.+@(.+):(\d+).*")
(def ^:private unauthenticated-proxy-re #"(.+):(\d+).*")

(defn proxy-info [m]
  {:host (nth m 1)
   :port (nth m 2)})

(defn parse-proxy-info
  [s]
  (when s
    (let [p (cond
              (str/starts-with? s "http://") (subs s 7)
              (str/starts-with? s "https://") (subs s 8)
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

(defn env-proxy-info
  "Returns a map with proxy information parsed from env vars. The map
   will contain :http-proxy and :https-proxy entries if the relevant
   env vars are set and parsed correctly. The value for each is a map
   with :host and :port entries."
  []
  (let [http-proxy  (parse-proxy-info (or (*getenv-fn* "http_proxy")
                                          (*getenv-fn* "HTTP_PROXY")))
        https-proxy (parse-proxy-info (or (*getenv-fn* "https_proxy")
                                          (*getenv-fn* "HTTPS_PROXY")))]
    (cond-> {}
      http-proxy (assoc :http-proxy http-proxy)
      https-proxy (assoc :https-proxy https-proxy))))

(defn set-proxy-system-props!
  "Sets the proxy system properties in the current JVM.
   proxy-info parameter is as returned from env-proxy-info."
  [proxy-info]
  (let [{:keys [http-proxy https-proxy]} proxy-info]
    (when http-proxy
      (System/setProperty "http.proxyHost" (:host http-proxy))
      (System/setProperty "http.proxyPort" (:port http-proxy)))
    (when https-proxy
      (System/setProperty "https.proxyHost" (:host https-proxy))
      (System/setProperty "https.proxyPort" (:port https-proxy)))))


(defn clojure-tools-download-direct
  "Downloads from SOURCE url to DEST file returning true on success."
  [source dest]
  (try
    (set-proxy-system-props! (env-proxy-info))
    (let [source (URL. source)
          dest (io/file dest)
          conn ^URLConnection (.openConnection ^URL source)]
      (when (instance? HttpURLConnection conn)
        (.setInstanceFollowRedirects #^java.net.HttpURLConnection conn true))
      (.connect conn)
      (with-open [is (.getInputStream conn)]
        (io/copy is dest))
      true)
    (catch Exception e
      (warn ::direct-download (.getMessage e))
      false)))

(def ^:private clojure-tools-info*
  "A delay'd map with information about the clojure tools archive, where
  to download it from, which files to extract and where to.

  The map contains:

  :ct-base-dir The relative top dir name to extract the archive files to.

  :ct-error-exit-code The process exit code to return if the archive
  cannot be downloaded.

  :ct-aux-files-names Other important files in the archive.

  :ct-jar-name The main clojure tools jar file in the archive.

  :ct-url-str The url to download the archive from.

  :ct-zip-name The file name to store the archive as."
  (delay (let [version @version]
           {:ct-base-dir  "ClojureTools"
            :ct-error-exit-code 99
            :ct-aux-files-names ["exec.jar" "example-deps.edn" "tools.edn"]
            :ct-jar-name (format "clojure-tools-%s.jar" version)
            :ct-url-str (format "https://download.clojure.org/install/clojure-tools-%s.zip" version)
            :ct-zip-name "tools.zip"})))

(defn unzip
  [zip-file destination-dir]
  (let [{:keys [ct-aux-files-names ct-jar-name]} @clojure-tools-info*
        zip-file (io/file zip-file)
        destination-dir (io/file destination-dir)
        _ (.mkdirs destination-dir)
        destination-dir (.toPath destination-dir)
        zip-file (.toPath zip-file)
        files (into #{ct-jar-name} ct-aux-files-names)]
    (with-open
      [fis (Files/newInputStream zip-file (into-array java.nio.file.OpenOption []))
       zis (ZipInputStream. fis)]
      (loop []
        (when-let [entry (.getNextEntry zis)]
          (let [entry-name (.getName entry)
                file-name (.getName (io/file entry-name))]
            (when (contains? files file-name)
              (let [new-path (.resolve destination-dir file-name)]
                (Files/copy ^java.io.InputStream zis
                            new-path
                            ^"[Ljava.nio.file.CopyOption;"
                            (into-array CopyOption
                                        [java.nio.file.StandardCopyOption/REPLACE_EXISTING]))))
            (recur)))))))

(defn- clojure-tools-java-downloader-spit
  "Spits out and returns the path to `ClojureToolsDownloader.java` file
  in DEST-DIR'ectory that when invoked (presumambly by the `java`
  executable directly) with a source URL and destination file path in
  args[0] and args[1] respectively, will download the source to
  destination. No arguments validation is performed and returns exit
  code 1 on failure."
  [dest-dir]
  (let [dest-file (.getCanonicalPath (io/file dest-dir "ClojureToolsDownloader.java"))]
    (spit dest-file
          (str "
/** Auto-generated by " *file* ". **/
package borkdude.deps;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
public class ClojureToolsDownloader {
    public static void main (String[] args) {
    try {
        URL url = new URL(args[0]);
//        System.err.println (\":0 \" +args [0]+ \" :1 \"+args [1]);
        URLConnection conn = url.openConnection();
        if (conn instanceof HttpURLConnection)
           {((HttpURLConnection) conn).setInstanceFollowRedirects(true);}
        ReadableByteChannel readableByteChannel = Channels.newChannel(conn.getInputStream());
        FileOutputStream fileOutputStream = new FileOutputStream(args[1]);
        FileChannel fileChannel = fileOutputStream.getChannel();
        fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        System.exit(0);
    } catch (IOException e) {
        e.printStackTrace();
        System.exit(1); }}}"))
    dest-file))

(defn- clojure-tools-download-java
  "Downloads URL file to DEST-ZIP-FILE by invoking `java` with JVM-OPTS
  on a `.java` program file, and returns true on success. Requires
  Java 11+ (JEP 330)."
  [url dest-zip-file jvm-opts]
  (let [dest-dir (.getCanonicalPath (io/file dest-zip-file ".."))
        dlr-path (clojure-tools-java-downloader-spit dest-dir)
        java-cmd [(get-java-cmd) "-XX:-OmitStackTraceInFastThrow"]
        success?* (atom true)]
    (binding [*exit-fn* (fn
                          ([exit-code] (when-not (= exit-code 0) (reset! success?* false)))
                          ([exit-code msg] (when-not (= exit-code 0)
                                             (warn msg)
                                             (reset! success?* false))))]
      (shell-command (vec (concat java-cmd
                                  jvm-opts
                                  [dlr-path url (str dest-zip-file)])))
      (io/delete-file dlr-path true)
      @success?*)))

(defn clojure-tools-jar-download
  "Downloads clojure tools archive in OUT-DIR, if not already there,
  and extracts in-place the clojure tools jar file and other important
  files.

  The download is attempted directly from this process, unless
  JAVA-ARGS-WITH-CLJ-JVM-OPTS is set, in which case a java subprocess
  is created to download the archive passing in its value as command
  line options.

  It calls `*exit-fn*` if it cannot download the archive, with
  instructions how to manually download it."
  [out-dir java-args-with-clj-jvm-opts {:keys [debug] :as _opts}]
  (let [{:keys [ct-error-exit-code ct-url-str ct-zip-name]} @clojure-tools-info*
        dir (io/file out-dir)
        zip-file (io/file out-dir ct-zip-name)]
    (when-not (.exists zip-file)
      (warn "Downloading" ct-url-str "to" (str zip-file))
      (.mkdirs dir)
      (or (when java-args-with-clj-jvm-opts
            (when debug (warn "Attempting download using java subprocess... (requires Java11+"))
            (clojure-tools-download-java ct-url-str (str zip-file) java-args-with-clj-jvm-opts))
          (do (when debug (warn "Attempting direct download..."))
              (clojure-tools-download-direct ct-url-str zip-file))
          (*exit-fn* ct-error-exit-code (str "Error: Cannot download Clojure tools."
                                             " Please download manually from " ct-url-str
                                             " to " (str (io/file dir ct-zip-name))))
          {:url ct-url-str :dest-dir (str dir)}))
    (warn "Unzipping" (str zip-file) "...")
    (unzip zip-file (.getPath dir))
    (.delete zip-file))
  (warn "Successfully installed clojure tools!"))

(defn jvm-proxy-settings
  "Returns a vector containing the JVM args to be passed to a new process
   to set its proxy system properties.
   proxy-info parameter is as returned from env-proxy-info."
  [proxy-info]
  (let [{:keys [http-proxy https-proxy]} proxy-info]
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
          (do (warn arg "-R is no longer supported, use -A with repl, -M for main, -X for exec, -T for tool")
              (*exit-fn* 1))
          (some #(str/starts-with? arg %) ["-O"])
          (let [msg (str arg " is no longer supported, use -A with repl, -M for main, -X for exec, -T for tool")]
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


(defn- as-path
  ^Path [path]
  (if (instance? Path path) path
      (.toPath (io/file path))))

(defn unixify
  ^Path [f]
  (as-path (if windows?
             (-> f as-path .toUri .getPath)
             (str f))))

(defn- relativize
  "Returns relative path by comparing this with other. Returns absolute path unchanged."
  ^Path [f]
  (if (.isAbsolute (as-path f))
    f
    (if-let [dir *dir*]
      (str (.relativize (unixify (.toAbsolutePath (as-path dir)))
                        (unixify (.toAbsolutePath (as-path f)))))
      f)))

(defn -main
  "See `help-text`.

  In addition

  - the values of the `CLJ_JVM_OPTS` and `JAVA_OPTIONS` environment
  variables are passed to the java subprocess as command line options
  when downloading dependencies and running any other commands
  respectively.

  - if the clojure tools jar cannot be located and the clojure tools
  archive is not found, an attempt is made to download the archive
  from the official site and extract its contents locally. The archive
  is downloaded from this process directly, unless the `CLJ_JVM_OPTS`
  env variable is set and a succesful attempt is made to download the
  archive by invoking a java subprocess passing the env variable value
  as command line options."
  [& command-line-args]
  (let [opts (parse-args command-line-args)
        {:keys [ct-base-dir ct-jar-name]} @clojure-tools-info*
        debug (*getenv-fn* "DEPS_CLJ_DEBUG")
        java-cmd [(get-java-cmd) "-XX:-OmitStackTraceInFastThrow"]
        env-tools-dir (or
                       ;; legacy name
                       (*getenv-fn* "CLOJURE_TOOLS_DIR")
                       (*getenv-fn* "DEPS_CLJ_TOOLS_DIR"))
        tools-dir (or env-tools-dir
                      (.getPath (io/file (home-dir)
                                         ".deps.clj"
                                         @version
                                         ct-base-dir)))
        install-dir tools-dir
        libexec-dir (if env-tools-dir
                      (let [f (io/file env-tools-dir "libexec")]
                        (if (.exists f)
                          (.getPath f)
                          env-tools-dir))
                      tools-dir)
        tools-jar (io/file libexec-dir ct-jar-name)
        exec-jar (io/file libexec-dir "exec.jar")
        proxy-settings (jvm-proxy-settings (env-proxy-info))
        clj-jvm-opts (some-> (*getenv-fn* "CLJ_JVM_OPTS") (str/split #" "))
        tools-cp
        (or
         (when (.exists tools-jar) (.getPath tools-jar))
         (binding [*out* *err*]
           (warn "Clojure tools not yet in expected location:" (str tools-jar))
           (let [java-clj-jvm-opts (when clj-jvm-opts (vec (concat clj-jvm-opts
                                                                   proxy-settings)))]
             (clojure-tools-jar-download libexec-dir java-clj-jvm-opts {:debug debug}))
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
        (vec (concat java-cmd
                     clj-jvm-opts
                     proxy-settings
                     ["-classpath" tools-cp "clojure.main"]))
        config-dir
        (or (*getenv-fn* "CLJ_CONFIG")
            (when-let [xdg-config-home (*getenv-fn* "XDG_CONFIG_HOME")]
              (.getPath (io/file xdg-config-home "clojure")))
            (.getPath (io/file (home-dir) ".clojure")))
        java-opts (some-> (*getenv-fn* "JAVA_OPTS") (str/split #" "))]
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
    (let [config-tools-edn (io/file config-dir "tools" "tools.edn")
          install-tools-edn (io/file install-dir "tools.edn")]
      (when (and install-dir
                 (not (.exists config-tools-edn))
                 (.exists install-tools-edn))
        (io/make-parents config-tools-edn)
        (io/copy install-tools-edn config-tools-edn)))
    ;; Determine user cache directory
    (let [user-cache-dir
          (or (*getenv-fn* "CLJ_CACHE")
              (when-let [xdg-config-home (*getenv-fn* "XDG_CACHE_HOME")]
                (.getPath (io/file xdg-config-home "clojure")))
              (.getPath (io/file config-dir ".cpcache")))
          ;; Chain deps.edn in config paths. repro=skip config dir
          config-user
          (when-not (:repro opts)
            (.getPath (io/file config-dir "deps.edn")))
          config-project deps-edn
          config-paths
          (if (:repro opts)
            (if install-dir
              [(.getPath (io/file install-dir "deps.edn")) deps-edn]
              [deps-edn])
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
                    (concat [cache-version]
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
          cp-file (.getPath (io/file cache-dir (str ck ".cp")))
          jvm-file (.getPath (io/file cache-dir (str ck ".jvm")))
          main-file (.getPath (io/file cache-dir (str ck ".main")))
          basis-file (.getPath (io/file cache-dir (str ck ".basis")))
          manifest-file (.getPath (io/file cache-dir (str ck ".manifest")))
          _ (when (:verbose opts)
              (println "deps.clj version =" deps-clj-version)
              (println "version          =" @version)
              (when install-dir (println "install_dir      =" install-dir))
              (println "config_dir       =" config-dir)
              (println "config_paths     =" (str/join " " config-paths))
              (println "cache_dir        =" cache-dir)
              (println "cp_file          =" cp-file)
              (println))
          tree? (:tree opts)
          ;; Check for stale classpath file
          cp-file (io/file cp-file)
          stale
          (or (:force opts)
              (:trace opts)
              tree?
              (:prep opts)
              (not (.exists cp-file))
              (when tool-name
                (let [tool-file (io/file config-dir "tools" (str tool-name ".edn"))]
                  (when (.exists tool-file)
                    (> (.lastModified tool-file)
                       (.lastModified cp-file)))))
              (some (fn [config-path]
                      (let [f (io/file config-path)]
                        (when (.exists f)
                          (> (.lastModified f)
                             (.lastModified cp-file))))) config-paths)
              ;; Are deps.edn files stale?
              (when (.exists (io/file manifest-file))
                (let [manifests (-> manifest-file slurp str/split-lines)]
                  (some (fn [manifest]
                          (let [f (io/file manifest)]
                            (or (not (.exists f))
                                (> (.lastModified f)
                                   (.lastModified cp-file))))) manifests)))
              ;; Are .jar files in classpath missing?
              (let [cp (slurp cp-file)
                    entries (vec (.split ^String cp java.io.File/pathSeparator))]
                (some (fn [entry]
                        (when (str/ends-with? entry ".jar")
                          (not (.exists (io/file entry)))))
                      entries)))
          tools-args
          (when (or stale (:pom opts))
            (cond-> []
              (not (str/blank? (:deps-data opts)))
              (conj "--config-data" (:deps-data opts))
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
              (conj "--tree")))
          classpath-not-needed? (boolean (some #(% opts) [:describe :help :version]))]
      ;;  If stale, run make-classpath to refresh cached classpath
      (when (and stale (not classpath-not-needed?))
        (when (:verbose opts)
          (warn "Refreshing classpath"))
        (let [res (shell-command (into clj-main-cmd
                                       (concat
                                        ["-m" "clojure.tools.deps.script.make-classpath2"
                                         "--config-user" config-user
                                         "--config-project" (relativize config-project)
                                         "--basis-file" (relativize basis-file)
                                         "--cp-file" (relativize cp-file)
                                         "--jvm-file" (relativize jvm-file)
                                         "--main-file" (relativize main-file)
                                         "--manifest-file" (relativize manifest-file)]
                                        tools-args))
                                 {:to-string? tree?})]
          (when tree?
            (print res) (flush))))
      (let [cp (cond (or classpath-not-needed?
                         (:prep opts)) nil
                     (not (str/blank? (:force-cp opts))) (:force-cp opts)
                     :else (slurp cp-file))]
        (cond (:help opts) (do (println @help-text)
                               (*exit-fn* 0))
              (:version opts) (do (println "Clojure CLI version (deps.clj)" @version)
                                  (*exit-fn* 0))
              (:prep opts) (*exit-fn* 0)
              (:pom opts)
              (shell-command (into clj-main-cmd
                                   ["-m" "clojure.tools.deps.script.generate-manifest2"
                                    "--config-user" config-user
                                    "--config-project" (relativize config-project)
                                    "--gen=pom" (str/join " " tools-args)]))
              (:print-classpath opts)
              (println cp)
              (:describe opts)
              (describe [[:deps-clj-version deps-clj-version]
                         [:version @version]
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
                    main-args (concat java-cmd
                                      java-opts
                                      proxy-settings
                                      jvm-cache-opts
                                      (:jvm-opts opts)
                                      [(str "-Dclojure.basis=" (relativize basis-file))
                                       "-classpath" cp
                                       "clojure.main"]
                                      main-opts)
                    main-args (filterv some? main-args)
                    main-args (into main-args (:args opts))]
                (when (and (= :repl mode)
                           (pos? (count (:args opts))))
                  (warn "WARNING: Implicit use of clojure.main with options is deprecated, use -M"))
                (*process-fn* main-args)))))))
