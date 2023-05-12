# Table of contents
-  [`borkdude.deps`](#borkdude.deps)  - Port of https://github.com/clojure/brew-install/blob/1.11.1/src/main/resources/clojure/install/clojure in Clojure.
    -  [`*aux-process-fn*`](#borkdude.deps/*aux-process-fn*) - Invokes <code>java</code> with arguments to calculate classpath, etc.
    -  [`*clojure-process-fn*`](#borkdude.deps/*clojure-process-fn*) - Invokes <code>java</code> with arguments to <code>clojure.main</code> to start Clojure.
    -  [`*clojure-tools-download-fn*`](#borkdude.deps/*clojure-tools-download-fn*) - Can be dynamically rebound to customise the download of the Clojure tools.
    -  [`*dir*`](#borkdude.deps/*dir*) - Directory in which deps.clj should be executed.
    -  [`*exit-fn*`](#borkdude.deps/*exit-fn*) - Function that is called on exit with <code>:exit</code> code and <code>:message</code>, an exceptional message when exit is non-zero.
    -  [`-main`](#borkdude.deps/-main) - See <code>help-text</code>.
    -  [`clojure-tools-download!`](#borkdude.deps/clojure-tools-download!) - Downloads clojure tools archive in <code>:out-dir</code>, if not already there, and extracts in-place the clojure tools jar file and other important files.
    -  [`clojure-tools-download-direct!`](#borkdude.deps/clojure-tools-download-direct!) - Downloads from <code>:url</code> to <code>:dest</code> file returning true on success.
    -  [`clojure-tools-download-java!`](#borkdude.deps/clojure-tools-download-java!) - Downloads <code>:url</code> zip file to <code>:dest</code> by invoking <code>java</code> with <code>:proxy</code> options on a <code>.java</code> program file, and returns true on success.
    -  [`deps-clj-version`](#borkdude.deps/deps-clj-version) - The current version of deps.clj.
    -  [`get-cache-dir`](#borkdude.deps/get-cache-dir) - Returns cache dir (<code>.cpcache</code>) from either local dir, if <code>deps-edn</code> exists, or the user cache dir.
    -  [`get-config-dir`](#borkdude.deps/get-config-dir) - Retrieves configuration directory.
    -  [`get-config-paths`](#borkdude.deps/get-config-paths) - Returns vec of configuration paths, i.e.
    -  [`get-help`](#borkdude.deps/get-help) - Returns help text as string.
    -  [`get-install-dir`](#borkdude.deps/get-install-dir) - Retrieves the install directory where tools jar is located (after download).
    -  [`get-local-deps-edn`](#borkdude.deps/get-local-deps-edn) - Returns the path of the <code>deps.edn</code> file (as string) in the current directory or as set by <code>-Sdeps-file</code>.
    -  [`get-proxy-info`](#borkdude.deps/get-proxy-info) - Returns a map with proxy information parsed from env vars.
    -  [`parse-cli-opts`](#borkdude.deps/parse-cli-opts) - Parses the command line options.
    -  [`print-help`](#borkdude.deps/print-help) - Print help text.
    -  [`proxy-jvm-opts`](#borkdude.deps/proxy-jvm-opts) - Returns a vector containing the JVM system property arguments to be passed to a new process to set its proxy system properties.
    -  [`set-proxy-system-props!`](#borkdude.deps/set-proxy-system-props!) - Sets the proxy system properties in the current JVM.

-----
# <a name="borkdude.deps">borkdude.deps</a>


Port of https://github.com/clojure/brew-install/blob/1.11.1/src/main/resources/clojure/install/clojure in Clojure




## <a name="borkdude.deps/*aux-process-fn*">`*aux-process-fn*`</a><a name="borkdude.deps/*aux-process-fn*"></a>
``` clojure

(*aux-process-fn* {:keys [cmd out]})
```

Invokes `java` with arguments to calculate classpath, etc. May be
  replaced by rebinding this dynamic var.

  Called with a map of:

  - `:cmd`: a vector of strings
  - `:out`: if set to `:string`, `:out` key in result must contains stdout

  Returns a map of:

  - `:exit`, the exit code of the process
  - `:out`, the string of stdout, if the input `:out` was set to `:string`
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L115-L129">Source</a></sub></p>

## <a name="borkdude.deps/*clojure-process-fn*">`*clojure-process-fn*`</a><a name="borkdude.deps/*clojure-process-fn*"></a>
``` clojure

(*clojure-process-fn* {:keys [cmd]})
```

Invokes `java` with arguments to `clojure.main` to start Clojure. May
  be replaced by rebinding this dynamic var.

  Called with a map of:

  - `:cmd`: a vector of strings

  Must return a map of `:exit`, the exit code of the process.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L131-L141">Source</a></sub></p>

## <a name="borkdude.deps/*clojure-tools-download-fn*">`*clojure-tools-download-fn*`</a><a name="borkdude.deps/*clojure-tools-download-fn*"></a>




Can be dynamically rebound to customise the download of the Clojure tools.
   Should be bound to a function accepting a map with:
   - `:url`: The URL to download, as a string
   - `:dest`: The path to the file to download it to, as a string
   - `:proxy-opts`: a map as returned by [`get-proxy-info`](#borkdude.deps/get-proxy-info)
   - `:clj-jvm-opts`: a vector of JVM opts (as passed on the command line).

  Should return `true` if the download was successful, or false if not.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L478-L487">Source</a></sub></p>

## <a name="borkdude.deps/*dir*">`*dir*`</a><a name="borkdude.deps/*dir*"></a>




Directory in which deps.clj should be executed.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L50-L51">Source</a></sub></p>

## <a name="borkdude.deps/*exit-fn*">`*exit-fn*`</a><a name="borkdude.deps/*exit-fn*"></a>
``` clojure

(*exit-fn* {:keys [exit message]})
```

Function that is called on exit with `:exit` code and `:message`, an exceptional message when exit is non-zero
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L39-L43">Source</a></sub></p>

## <a name="borkdude.deps/-main">`-main`</a><a name="borkdude.deps/-main"></a>
``` clojure

(-main & command-line-args)
```

See [`help-text`](#borkdude.deps/help-text).

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
  as command line options.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L745-L992">Source</a></sub></p>

## <a name="borkdude.deps/clojure-tools-download!">`clojure-tools-download!`</a><a name="borkdude.deps/clojure-tools-download!"></a>
``` clojure

(clojure-tools-download! {:keys [out-dir debug proxy-opts clj-jvm-opts]})
```

Downloads clojure tools archive in `:out-dir`, if not already there,
  and extracts in-place the clojure tools jar file and other important
  files.

  The download is attempted directly from this process, unless
  `:jvm-opts` is set, in which case a java subprocess
  is created to download the archive passing in its value as command
  line options.

  It calls [`*exit-fn*`](#borkdude.deps/*exit-fn*) if it cannot download the archive, with
  instructions how to manually download it.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L489-L524">Source</a></sub></p>

## <a name="borkdude.deps/clojure-tools-download-direct!">`clojure-tools-download-direct!`</a><a name="borkdude.deps/clojure-tools-download-direct!"></a>
``` clojure

(clojure-tools-download-direct! {:keys [url dest]})
```

Downloads from `:url` to `:dest` file returning true on success.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L339-L355">Source</a></sub></p>

## <a name="borkdude.deps/clojure-tools-download-java!">`clojure-tools-download-java!`</a><a name="borkdude.deps/clojure-tools-download-java!"></a>
``` clojure

(clojure-tools-download-java! {:keys [url dest proxy-opts clj-jvm-opts]})
```

Downloads `:url` zip file to `:dest` by invoking `java` with
  `:proxy` options on a `.java` program file, and returns true on
  success. Requires Java 11+ (JEP 330).
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L457-L475">Source</a></sub></p>

## <a name="borkdude.deps/deps-clj-version">`deps-clj-version`</a><a name="borkdude.deps/deps-clj-version"></a>




The current version of deps.clj
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L23-L27">Source</a></sub></p>

## <a name="borkdude.deps/get-cache-dir">`get-cache-dir`</a><a name="borkdude.deps/get-cache-dir"></a>
``` clojure

(get-cache-dir {:keys [deps-edn config-dir]})
```

Returns cache dir (`.cpcache`) from either local dir, if `deps-edn`
  exists, or the user cache dir.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L688-L699">Source</a></sub></p>

## <a name="borkdude.deps/get-config-dir">`get-config-dir`</a><a name="borkdude.deps/get-config-dir"></a>
``` clojure

(get-config-dir)
```

Retrieves configuration directory.
  First tries `CLJ_CONFIG` env var, then `$XDG_CONFIG_HOME/clojure`, then ~/.clojure.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L671-L678">Source</a></sub></p>

## <a name="borkdude.deps/get-config-paths">`get-config-paths`</a><a name="borkdude.deps/get-config-paths"></a>
``` clojure

(get-config-paths {:keys [cli-opts deps-edn config-dir install-dir]})
```

Returns vec of configuration paths, i.e. deps.edn from:
  - `:install-dir` as obtained thrhough [`get-install-dir`](#borkdude.deps/get-install-dir)
  - `:config-dir` as obtained through [`get-config-dir`](#borkdude.deps/get-config-dir)
  - `:deps-edn` as obtained through [`get-local-deps-edn`](#borkdude.deps/get-local-deps-edn)
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L701-L716">Source</a></sub></p>

## <a name="borkdude.deps/get-help">`get-help`</a><a name="borkdude.deps/get-help"></a>
``` clojure

(get-help)
```

Returns help text as string.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L735-L738">Source</a></sub></p>

## <a name="borkdude.deps/get-install-dir">`get-install-dir`</a><a name="borkdude.deps/get-install-dir"></a>
``` clojure

(get-install-dir)
```

Retrieves the install directory where tools jar is located (after download).
  Defaults to ~/.deps.clj/<version>/ClojureTools.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L660-L669">Source</a></sub></p>

## <a name="borkdude.deps/get-local-deps-edn">`get-local-deps-edn`</a><a name="borkdude.deps/get-local-deps-edn"></a>
``` clojure

(get-local-deps-edn {:keys [cli-opts]})
```

Returns the path of the `deps.edn` file (as string) in the current directory or as set by `-Sdeps-file`.
  Required options:
  * `:cli-opts`: command line options as parsed by `parse-opts`
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L680-L686">Source</a></sub></p>

## <a name="borkdude.deps/get-proxy-info">`get-proxy-info`</a><a name="borkdude.deps/get-proxy-info"></a>
``` clojure

(get-proxy-info)
```

Returns a map with proxy information parsed from env vars. The map
   will contain :http-proxy and :https-proxy entries if the relevant
   env vars are set and parsed correctly. The value for each is a map
   with :host and :port entries.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L314-L326">Source</a></sub></p>

## <a name="borkdude.deps/parse-cli-opts">`parse-cli-opts`</a><a name="borkdude.deps/parse-cli-opts"></a>
``` clojure

(parse-cli-opts args)
```

Parses the command line options.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L556-L627">Source</a></sub></p>

## <a name="borkdude.deps/print-help">`print-help`</a><a name="borkdude.deps/print-help"></a>
``` clojure

(print-help)
```

Print help text
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L740-L743">Source</a></sub></p>

## <a name="borkdude.deps/proxy-jvm-opts">`proxy-jvm-opts`</a><a name="borkdude.deps/proxy-jvm-opts"></a>
``` clojure

(proxy-jvm-opts {:keys [http-proxy https-proxy]})
```

Returns a vector containing the JVM system property arguments to be passed to a new process
   to set its proxy system properties.
   proxy-info parameter is as returned from env-proxy-info.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L446-L455">Source</a></sub></p>

## <a name="borkdude.deps/set-proxy-system-props!">`set-proxy-system-props!`</a><a name="borkdude.deps/set-proxy-system-props!"></a>
``` clojure

(set-proxy-system-props! {:keys [http-proxy https-proxy]})
```

Sets the proxy system properties in the current JVM.
   proxy-info parameter is as returned from env-proxy-info.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L328-L337">Source</a></sub></p>
