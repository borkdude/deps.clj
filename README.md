# deps.clj

[![CircleCI](https://circleci.com/gh/borkdude/deps.clj/tree/master.svg?style=shield)](https://circleci.com/gh/borkdude/deps.clj/tree/master)
[![Clojars Project](https://img.shields.io/clojars/v/borkdude/deps.clj.svg)](https://clojars.org/borkdude/deps.clj)

A port of the [clojure](https://github.com/clojure/brew-install/) bash script to
Clojure.

This script can be run on the JVM or with [babashka](https://github.com/borkdude/babashka/). The latter makes it go about as fast as the original bash version.

## Rationale

TBD.

## Status

Experimental, breaking changes will happen. Feedback is welcome.

## Non-standard options

The `deps.clj` script adds the following non-standard options:

```
 -Sdeps-file    Use this file instead of deps.edn
 -Scommand      A custom command that will be invoked. Substitutions: {{classpath}}, {{main-opts}}.
```

## Example usage

Given this `script-deps.edn` file:

``` clojure
{:paths ["scripts"]
 :aliases
 {:main
  {:main-opts ["-m" "scripts.main"]}}}
```

and `scripts/main.cljc`:

``` clojure
(ns scripts.main)

(defn -main [& _args]
  (println "Hello from script!"))
```

you can invoke `deps.clj` as follows to invoke [babashka](https://github.com/borkdude/babashka/):

``` shell
$ deps.clj -Sdeps-file script-deps.edn -A:main -Scommand "bb -cp {{classpath}} {{main-opts}}"
Hello from script!
```

This can also be used with [planck](https://github.com/planck-repl/planck):

``` shell
$ deps.clj -Sdeps-file script-deps.edn -A:main -Scommand "planck --classpath {{classpath}} {{main-opts}}"
Hello from script!
```

## License

Copyright © 2019 Michiel Borkent

Distributed under the EPL License. See LICENSE.

This project is based on code from
[clojure/brew-install](https://github.com/clojure/brew-install/) which is
licensed under the same EPL License.
