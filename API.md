# Table of contents
-  [`borkdude.deps`](#borkdude.deps)  - Port of https://github.com/clojure/brew-install/blob/1.11.1/src/main/resources/clojure/install/clojure in Clojure.
    -  [`-main`](#borkdude.deps/-main) - See <code>help-text</code>.
    -  [`cache-version`](#borkdude.deps/cache-version)
    -  [`clojure-tools-download-direct!`](#borkdude.deps/clojure-tools-download-direct!) - Downloads from <code>:url</code> to <code>:dest</code> file returning true on success.
    -  [`clojure-tools-download-jar!`](#borkdude.deps/clojure-tools-download-jar!) - Downloads clojure tools archive in <code>:out-dir</code>, if not already there, and extracts in-place the clojure tools jar file and other important files.
    -  [`deps-clj-version`](#borkdude.deps/deps-clj-version)
    -  [`get-cache-dir`](#borkdude.deps/get-cache-dir) - Returns cache dir (<code>.cpcache</code>) from either local dir, if <code>deps-edn</code> exists, or the user cache dir.
    -  [`get-config-dir`](#borkdude.deps/get-config-dir) - Retrieves configuration directory.
    -  [`get-config-paths`](#borkdude.deps/get-config-paths)
    -  [`get-env-tools-dir`](#borkdude.deps/get-env-tools-dir) - Retrieves the tools-directory from environment variable <code>DEPS_CLJ_TOOLS_DIR</code>.
    -  [`get-local-deps-edn`](#borkdude.deps/get-local-deps-edn) - Returns the path of the <code>deps.edn</code> file (as string) in the current directory or as set by <code>-Sdeps-file</code>.
    -  [`get-proxy-info`](#borkdude.deps/get-proxy-info) - Returns a map with proxy information parsed from env vars.
    -  [`get-tools-dir`](#borkdude.deps/get-tools-dir) - Retrieves the tools directory where tools jar is located (after download).
    -  [`help-text`](#borkdude.deps/help-text)
    -  [`jvm-proxy-opts`](#borkdude.deps/jvm-proxy-opts) - Returns a vector containing the JVM args to be passed to a new process to set its proxy system properties.
    -  [`parse-cli-opts`](#borkdude.deps/parse-cli-opts) - Parses the command line options.
    -  [`set-proxy-system-props!`](#borkdude.deps/set-proxy-system-props!) - Sets the proxy system properties in the current JVM.

-----
# <a name="borkdude.deps">borkdude.deps</a>


Port of https://github.com/clojure/brew-install/blob/1.11.1/src/main/resources/clojure/install/clojure in Clojure




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
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L702-L949">Source</a></sub></p>

## <a name="borkdude.deps/cache-version">`cache-version`</a><a name="borkdude.deps/cache-version"></a>



<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L21-L21">Source</a></sub></p>

## <a name="borkdude.deps/clojure-tools-download-direct!">`clojure-tools-download-direct!`</a><a name="borkdude.deps/clojure-tools-download-direct!"></a>
``` clojure

(clojure-tools-download-direct! {:keys [url dest]})
```

Downloads from `:url` to `:dest` file returning true on success.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L313-L329">Source</a></sub></p>

## <a name="borkdude.deps/clojure-tools-download-jar!">`clojure-tools-download-jar!`</a><a name="borkdude.deps/clojure-tools-download-jar!"></a>
``` clojure

(clojure-tools-download-jar! {:keys [out-dir jvm-opts debug]})
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
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L450-L484">Source</a></sub></p>

## <a name="borkdude.deps/deps-clj-version">`deps-clj-version`</a><a name="borkdude.deps/deps-clj-version"></a>



<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L23-L26">Source</a></sub></p>

## <a name="borkdude.deps/get-cache-dir">`get-cache-dir`</a><a name="borkdude.deps/get-cache-dir"></a>
``` clojure

(get-cache-dir {:keys [deps-edn config-dir]})
```

Returns cache dir (`.cpcache`) from either local dir, if `deps-edn`
  exists, or the user cache dir.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L659-L670">Source</a></sub></p>

## <a name="borkdude.deps/get-config-dir">`get-config-dir`</a><a name="borkdude.deps/get-config-dir"></a>
``` clojure

(get-config-dir {:keys []})
```

Retrieves configuration directory.
  First tries `CLJ_CONFIG` env var, then `$XDG_CONFIG_HOME/clojure`, then ~/.clojure.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L642-L649">Source</a></sub></p>

## <a name="borkdude.deps/get-config-paths">`get-config-paths`</a><a name="borkdude.deps/get-config-paths"></a>
``` clojure

(get-config-paths {:keys [cli-opts deps-edn config-dir install-dir]})
```
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L672-L683">Source</a></sub></p>

## <a name="borkdude.deps/get-env-tools-dir">`get-env-tools-dir`</a><a name="borkdude.deps/get-env-tools-dir"></a>
``` clojure

(get-env-tools-dir)
```

Retrieves the tools-directory from environment variable `DEPS_CLJ_TOOLS_DIR`
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L623-L629">Source</a></sub></p>

## <a name="borkdude.deps/get-local-deps-edn">`get-local-deps-edn`</a><a name="borkdude.deps/get-local-deps-edn"></a>
``` clojure

(get-local-deps-edn {:keys [cli-opts]})
```

Returns the path of the `deps.edn` file (as string) in the current directory or as set by `-Sdeps-file`.
  Required options:
  * `:cli-opts`: command line options as parsed by `parse-opts`
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L651-L657">Source</a></sub></p>

## <a name="borkdude.deps/get-proxy-info">`get-proxy-info`</a><a name="borkdude.deps/get-proxy-info"></a>
``` clojure

(get-proxy-info)
```

Returns a map with proxy information parsed from env vars. The map
   will contain :http-proxy and :https-proxy entries if the relevant
   env vars are set and parsed correctly. The value for each is a map
   with :host and :port entries.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L288-L300">Source</a></sub></p>

## <a name="borkdude.deps/get-tools-dir">`get-tools-dir`</a><a name="borkdude.deps/get-tools-dir"></a>
``` clojure

(get-tools-dir {:keys []})
```

Retrieves the tools directory where tools jar is located (after download).
  Defaults to ~/.deps.clj/<version>/ClojureTools.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L631-L640">Source</a></sub></p>

## <a name="borkdude.deps/help-text">`help-text`</a><a name="borkdude.deps/help-text"></a>



<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L117-L197">Source</a></sub></p>

## <a name="borkdude.deps/jvm-proxy-opts">`jvm-proxy-opts`</a><a name="borkdude.deps/jvm-proxy-opts"></a>
``` clojure

(jvm-proxy-opts {:keys [http-proxy https-proxy]})
```

Returns a vector containing the JVM args to be passed to a new process
   to set its proxy system properties.
   proxy-info parameter is as returned from env-proxy-info.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L486-L495">Source</a></sub></p>

## <a name="borkdude.deps/parse-cli-opts">`parse-cli-opts`</a><a name="borkdude.deps/parse-cli-opts"></a>
``` clojure

(parse-cli-opts args)
```

Parses the command line options.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L528-L599">Source</a></sub></p>

## <a name="borkdude.deps/set-proxy-system-props!">`set-proxy-system-props!`</a><a name="borkdude.deps/set-proxy-system-props!"></a>
``` clojure

(set-proxy-system-props! {:keys [http-proxy https-proxy]})
```

Sets the proxy system properties in the current JVM.
   proxy-info parameter is as returned from env-proxy-info.
<p><sub><a href="https://github.com/borkdude/deps.clj/blob/master/src/borkdude/deps.clj#L302-L311">Source</a></sub></p>
