# deps.clj

[![CircleCI](https://circleci.com/gh/borkdude/deps.clj/tree/master.svg?style=shield)](https://circleci.com/gh/borkdude/deps.clj/tree/master)
[![Build status](https://ci.appveyor.com/api/projects/status/wwfs4utm08dd9vx2/branch/master?svg=true)](https://ci.appveyor.com/project/borkdude/deps.clj/branch/master)
[![Clojars Project](https://img.shields.io/clojars/v/borkdude/deps.clj.svg)](https://clojars.org/borkdude/deps.clj)

Clojure provides the [`clojure`](https://clojure.org/guides/deps_and_cli)
command line tool for:

- Running an interactive REPL (Read-Eval-Print Loop)
- Running Clojure programs
- Evaluating Clojure expressions

The `clojure` tool is written in bash. This is a port of that tool written in
Clojure itself.

## Quickstart

``` shellsession
$ curl -sL https://github.com/borkdude/deps.clj/releases/download/v0.0.3/deps.clj-0.0.3-linux-amd64.zip -o deps.clj.zip
$ unzip deps.clj.zip
$ ./deps.exe -Spath
src:/Users/borkdude/.m2/repository/org/clojure/clojure/1.10.1/clojure-1.10.1.jar:...
```

## Rationale

If your `clojure` installation works well for you, then you don't need this.
This repo can be seen as a proof of concept of what is possible with GraalVM and Clojure. Windows users might find the `deps.exe` executable of value if they have trouble getting their system up and running. This repo might also be a place to experiment with features that are not available in the original `clojure` command.

Arguably Bash and Powershell are less attractive languages for Clojure
developers than Clojure itself. This repo provides the `clojure` bash script as
a port in Clojure. It can be used as a JVM Clojure script (`deps.clj`), uberjar
or library.

To get better startup, comparable to the original bash version, this project can be run as a binary called `deps.exe` or as a script called `deps.clj` with [babashka](https://github.com/borkdude/babashka/).

## Status

Experimental, but in a usable state. Breaking changes might happen to the non-standard functionality. Feedback and PRs are welcome.

## Installation

The `deps.clj` script can simply be downloaded from this repo. If you want a
binary or uberjar, go to
[releases](https://github.com/borkdude/deps.clj/releases).

You don't need `clojure` to run this project, but you will need a
`clojure-tools` jar. The `clojure-tools` jar is normally downloaded by the
`clojure` installer itself
([gz](https://download.clojure.org/install/clojure-tools-1.10.1.492.tar.gz),
[zip](https://download.clojure.org/install/clojure-tools-1.10.1.492.zip)). The
`deps.clj` tool will try to resolve the jar in the directory
`$HOME/.deps.clj/ClojureTools`. If it cannot it find it there, it will try to
download it. You can override the location of the jar with the
`CLOJURE_TOOLS_CP` environment variable.

### Windows

On Windows, you might want to install `deps.clj` using
[scoop](https://github.com/littleli/scoop-clojure).  When you get a message
about a missing `MSVCR100.dll`, also install the [Microsoft Visual C++ 2010
Redistributable Package
(x64)](https://www.microsoft.com/en-us/download/details.aspx?id=14632) which is
also available in the
[extras](https://github.com/lukesampson/scoop-extras/blob/master/bucket/vcredist2010.json)
Scoop bucket.

<img src="assets/windows-scoop.png">

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
