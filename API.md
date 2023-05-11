# Table of contents
-  [`borkdude.deps`](#borkdude.deps)  - Port of https://github.com/clojure/brew-install/blob/1.11.1/src/main/resources/clojure/install/clojure in Clojure.
    -  [`*exit-fn*`](#borkdude.deps/*exit-fn*) - Function that is called on exit with <code>:exit</code> code and <code>:message</code>, an exceptional message when exit is non-zero.
    -  [`-main`](#borkdude.deps/-main) - See <code>help-text</code>.
    -  [`clojure-tools-download!`](#borkdude.deps/clojure-tools-download!) - Downloads clojure tools archive in <code>:out-dir</code>, if not already there, and extracts in-place the clojure tools jar file and other important files.
    -  [`clojure-tools-download-direct!`](#borkdude.deps/clojure-tools-download-direct!) - Downloads from <code>:url</code> to <code>:dest</code> file returning true on success.
    -  [`clojure-tools-download-java!`](#borkdude.deps/clojure-tools-download-java!) - Downloads <code>:url</code> zip file to <code>:dest</code> by invoking <code>java</code> with <code>:proxy</code> options on a <code>.java</code> program file, and returns true on success.
    -  [`deps-clj-version`](#borkdude.deps/deps-clj-version) - The current version of deps.clj.
    -  [`get-cache-dir`](#borkdude.deps/get-cache-dir) - Returns cache dir (<code>.cpcache</code>) from either local dir, if <code>deps-edn</code> exists, or the user cache dir.
    -  [`get-config-dir`](#borkdude.deps/get-config-dir) - Retrieves configuration directory.
    -  [`get-config-paths`](#borkdude.deps/get-config-paths) - Returns vec of configuration paths, i.e.
    -  [`get-install-dir`](#borkdude.deps/get-install-dir) - Retrieves the install directory where tools jar is located (after download).
    -  [`get-local-deps-edn`](#borkdude.deps/get-local-deps-edn) - Returns the path of the <code>deps.edn</code> file (as string) in the current directory or as set by <code>-Sdeps-file</code>.
    -  [`get-proxy-info`](#borkdude.deps/get-proxy-info) - Returns a map with proxy information parsed from env vars.
    -  [`help-text`](#borkdude.deps/help-text)
    -  [`parse-cli-opts`](#borkdude.deps/parse-cli-opts) - Parses the command line options.
    -  [`proxy-jvm-opts`](#borkdude.deps/proxy-jvm-opts) - Returns a vector containing the JVM system property arguments to be passed to a new process to set its proxy system properties.
    -  [`set-proxy-system-props!`](#borkdude.deps/set-proxy-system-props!) - Sets the proxy system properties in the current JVM.

-----
# <a name="borkdude.deps">borkdude.deps</a>


Port of https://github.com/clojure/brew-install/blob/1.11.1/src/main/resources/clojure/install/clojure in Clojure




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
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L707-L953">Source</a></sub></p>

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
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L462-L497">Source</a></sub></p>

## <a name="borkdude.deps/clojure-tools-download-direct!">`clojure-tools-download-direct!`</a><a name="borkdude.deps/clojure-tools-download-direct!"></a>
``` clojure

(clojure-tools-download-direct! {:keys [url dest]})
```

Downloads from `:url` to `:dest` file returning true on success.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L313-L329">Source</a></sub></p>

## <a name="borkdude.deps/clojure-tools-download-java!">`clojure-tools-download-java!`</a><a name="borkdude.deps/clojure-tools-download-java!"></a>
``` clojure

(clojure-tools-download-java! {:keys [url dest proxy-opts clj-jvm-opts]})
```

Downloads `:url` zip file to `:dest` by invoking `java` with
  `:proxy` options on a `.java` program file, and returns true on
  success. Requires Java 11+ (JEP 330).
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L431-L449">Source</a></sub></p>

## <a name="borkdude.deps/deps-clj-version">`deps-clj-version`</a><a name="borkdude.deps/deps-clj-version"></a>




The current version of deps.clj
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L23-L27">Source</a></sub></p>

## <a name="borkdude.deps/get-cache-dir">`get-cache-dir`</a><a name="borkdude.deps/get-cache-dir"></a>
``` clojure

(get-cache-dir {:keys [deps-edn config-dir]})
```

Returns cache dir (`.cpcache`) from either local dir, if `deps-edn`
  exists, or the user cache dir.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L660-L671">Source</a></sub></p>

## <a name="borkdude.deps/get-config-dir">`get-config-dir`</a><a name="borkdude.deps/get-config-dir"></a>
``` clojure

(get-config-dir)
```

Retrieves configuration directory.
  First tries `CLJ_CONFIG` env var, then `$XDG_CONFIG_HOME/clojure`, then ~/.clojure.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L643-L650">Source</a></sub></p>

## <a name="borkdude.deps/get-config-paths">`get-config-paths`</a><a name="borkdude.deps/get-config-paths"></a>
``` clojure

(get-config-paths {:keys [cli-opts deps-edn config-dir install-dir]})
```

Returns vec of configuration paths, i.e. deps.edn from:
  - `:install-dir` as obtained thrhough [`get-install-dir`](#borkdude.deps/get-install-dir)
  - `:config-dir` as obtained through [`get-config-dir`](#borkdude.deps/get-config-dir)
  - `:deps-edn` as obtained through [`get-local-deps-edn`](#borkdude.deps/get-local-deps-edn)
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L673-L688">Source</a></sub></p>

## <a name="borkdude.deps/get-install-dir">`get-install-dir`</a><a name="borkdude.deps/get-install-dir"></a>
``` clojure

(get-install-dir)
```

Retrieves the install directory where tools jar is located (after download).
  Defaults to ~/.deps.clj/<version>/ClojureTools.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L632-L641">Source</a></sub></p>

## <a name="borkdude.deps/get-local-deps-edn">`get-local-deps-edn`</a><a name="borkdude.deps/get-local-deps-edn"></a>
``` clojure

(get-local-deps-edn {:keys [cli-opts]})
```

Returns the path of the `deps.edn` file (as string) in the current directory or as set by `-Sdeps-file`.
  Required options:
  * `:cli-opts`: command line options as parsed by `parse-opts`
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L652-L658">Source</a></sub></p>

## <a name="borkdude.deps/get-proxy-info">`get-proxy-info`</a><a name="borkdude.deps/get-proxy-info"></a>
``` clojure

(get-proxy-info)
```

Returns a map with proxy information parsed from env vars. The map
   will contain :http-proxy and :https-proxy entries if the relevant
   env vars are set and parsed correctly. The value for each is a map
   with :host and :port entries.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L288-L300">Source</a></sub></p>

## <a name="borkdude.deps/help-text">`help-text`</a><a name="borkdude.deps/help-text"></a>



<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L117-L197">Source</a></sub></p>

## <a name="borkdude.deps/parse-cli-opts">`parse-cli-opts`</a><a name="borkdude.deps/parse-cli-opts"></a>
``` clojure

(parse-cli-opts args)
```

Parses the command line options.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L529-L600">Source</a></sub></p>

## <a name="borkdude.deps/proxy-jvm-opts">`proxy-jvm-opts`</a><a name="borkdude.deps/proxy-jvm-opts"></a>
``` clojure

(proxy-jvm-opts {:keys [http-proxy https-proxy]})
```

Returns a vector containing the JVM system property arguments to be passed to a new process
   to set its proxy system properties.
   proxy-info parameter is as returned from env-proxy-info.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L420-L429">Source</a></sub></p>

## <a name="borkdude.deps/set-proxy-system-props!">`set-proxy-system-props!`</a><a name="borkdude.deps/set-proxy-system-props!"></a>
``` clojure

(set-proxy-system-props! {:keys [http-proxy https-proxy]})
```

Sets the proxy system properties in the current JVM.
   proxy-info parameter is as returned from env-proxy-info.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L302-L311">Source</a></sub></p>
