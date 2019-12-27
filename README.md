# deps.clj

[![CircleCI](https://circleci.com/gh/borkdude/deps.clj/tree/master.svg?style=shield)](https://circleci.com/gh/borkdude/deps.clj/tree/master)
[![Build status](https://ci.appveyor.com/api/projects/status/wwfs4utm08dd9vx2/branch/master?svg=true)](https://ci.appveyor.com/project/borkdude/deps.clj/branch/master)
[![Clojars Project](https://img.shields.io/clojars/v/borkdude/deps.clj.svg)](https://clojars.org/borkdude/deps.clj)

A port of the [clojure](https://github.com/clojure/brew-install/) bash script to
Clojure.

## Rationale

Arguably Bash and Powershell are less attractive languages for Clojure
developers than Clojure itself. This repo provides the `clojure` bash script as
a port in Clojure. It can be used as a JVM Clojure script (`deps.clj`), uberjar
or library.

To get better startup, comparable to the original bash version, the `deps.clj`
script can be run with [babashka](https://github.com/borkdude/babashka/) or as a
binary called `deps.exe`.

## Status

Experimental, breaking changes will happen. Feedback is welcome.

## Installation

The `deps.clj` script can simply be downloaded from this repo. If you want a
binary or uberjar, go to
[releases](https://github.com/borkdude/deps.clj/releases).

You don't need `clojure` to run this project, but you will need a
`clojure-tools` jar. This project will try to resolve the location of the
`clojure-tools` jar using the location of the `clojure` command. If you don't
have `clojure` installed, try setting the `CLOJURE_TOOLS_CP` environment
variable to the jar. The `clojure-tools` jar is normally downloaded by the
`clojure` installer itself
([gz](https://download.clojure.org/install/clojure-tools-1.10.1.492.tar.gz),
[zip](https://download.clojure.org/install/clojure-tools-1.10.1.492.zip)).

## Usage

### Script

``` shell
$ ./deps.clj -Spath -Sdeps '...'
```

### Linux / Mac

``` shell
$ ./deps.exe -Spath -Sdeps '...'
```

### Windows

An example of how to set `CLOJURE_TOOLS_CP` on Windows and how to pass an
`-Sdeps` value. Note the double double quotes.

``` shell
C:\Users\borkdude\Downloads>set CLOJURE_TOOLS_CP=c:\Users\borkdude\Downloads\clojure-tools-1.10.1.492\ClojureTools\clojure-tools-1.10.1.492.jar

C:\Users\borkdude\Downloads>deps -Spath -Sdeps "{:deps {borkdude/deps.clj {:mvn/version ""0.0.1""}}}"
```

## Non-standard options

The `deps.clj` script adds the following non-standard options:

```
 -Sdeps-file    Use this file instead of deps.edn
 -Scommand      A custom command that will be invoked. Substitutions: {{classpath}}, {{main-opts}}.
```

### Example usage

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
