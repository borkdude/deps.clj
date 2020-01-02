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
Clojure itself and comes in two flavors:

- as a script: `deps.clj`
- as an executable: `deps.exe`

## Quickstart

Linux and macOS:

``` shell
$ bash <(curl -s https://raw.githubusercontent.com/borkdude/deps.clj/master/install)
$ deps.exe
Clojure 1.10.1
user=>
```

Windows:

``` shell
C:\> PowerShell -Command "iwr -useb https://raw.githubusercontent.com/borkdude/deps.clj/master/install.ps1 | iex"
C:\> deps.exe
Clojure 1.10.1
user=>
```

## Rationale

Reasons why I made this:

- The port was done as proof of concept for
  [babashka](https://github.com/borkdude/babashka/). The entire bash script was
  ported to Clojure succesfully and runs just as fast with `bb`.

- This offers an arguably easier way to get going with `deps.edn` based projects
  in CI. Just download an installer script, execute it with bash or Powershell and you're set. Installer scripts are provided for linux, macOS and Windows.

- Windows users might find the `deps.exe` executable of value if they have
trouble getting their system up and running. It works with `cmd.exe` unlike the
current Powershell based approach.

- This repo might be a place to experiment with features that are not available
in the original `clojure` command. Most notably it offers an `-Scommand` option
which allows other programs to be started than just the JVM version of Clojure,
e.g. [`babashka`](https://github.com/borkdude/babashka/) or
[`planck`](https://github.com/planck-repl/planck/).

- Arguably bash and Powershell are less attractive languages for Clojure
developers than Clojure itself. This repo provides the `clojure` bash script as
a port in Clojure. It can be used as a binary, script (`deps.clj`), uberjar
or library.

- This repo can be seen as a proof of concept of what is possible with GraalVM
and Clojure.

## Status

Experimental, but in a usable state. Breaking changes might happen to the non-standard functionality. Feedback and PRs are welcome.

## Installation

There are three ways of running:

- as a compiled binary called `deps.exe` which is tailored to your OS
- as a script file called `deps.clj` using [`bb`](https://github.com/borkdude/babashka/) or `clojure`

### Binary

The binary version of deps.clj, called `deps.exe`, only requires a working
installation of `java`.

#### Manual download

Binaries for linux, macOS and Windows can be obtained on the [Github
releases](https://github.com/borkdude/deps.clj/releases) page.

#### Linux and macOS

Install using the installation script on linux or macOS:

``` shell
$ bash <(curl -s https://raw.githubusercontent.com/borkdude/deps.clj/master/install) /tmp
$ /tmp/deps.exe
Clojure 1.10.1
user=>
```

#### Windows

On Windows you might want to install `deps.clj` using
[scoop](https://github.com/littleli/scoop-clojure).

Alternatively you can install `deps.exe` using by executing the following line:

``` shell
C:\> PowerShell -Command "iwr -useb https://raw.githubusercontent.com/borkdude/deps.clj/master/install.ps1 | iex"
C:\> deps.exe
Clojure 1.10.1
user=>
```

It's automatically added to your path. In Powershell you can use it right away. In `cmd.exe` you'll have to restart the session for it to become available on the path.

When you get a message about a missing `MSVCR100.dll`, also install the
[Microsoft Visual C++ 2010 Redistributable Package
(x64)](https://www.microsoft.com/en-us/download/details.aspx?id=14632) which is
also available in the
[extras](https://github.com/lukesampson/scoop-extras/blob/master/bucket/vcredist2010.json)
Scoop bucket.

<img src="assets/windows-scoop.png">

### Script

The script, `deps.clj`, requires a working installation of `java` and
additionally [`bb`](https://github.com/borkdude/babashka/) or `clojure`.

It can simply be downloaded from this repo:

``` shell
$ curl -sL https://raw.githubusercontent.com/borkdude/deps.clj/master/deps.clj -o /tmp/deps.clj
$ chmod +x /tmp/deps.clj
$ bb /tmp/deps.clj
Clojure 1.10.1
user=>
```

### Tools jar

This project will look in `$HOME/.deps.clj/ClojureTools` for
`clojure-tools.jar`. If it cannot it find it there, it will try to download it
from [this](https://download.clojure.org/install/clojure-tools-1.10.1.492.zip)
location. You can override the location of the jar with the `CLOJURE_TOOLS_CP`
environment variable.

## Non-standard options

The `deps.clj` script adds the following non-standard options:

```
 -Sdeps-file    Use this file instead of deps.edn
 -Scommand      A custom command that will be invoked. Substitutions: {{classpath}}, {{main-opts}}.
```

### -Scommand

One of the use cases for `deps.clj` is invoking a different command than `java`.

Given this `deps.edn`:

``` clojure
{:aliases
 {:test
  {:extra-paths ["test"]
   :extra-deps {borkdude/spartan.test {:mvn/version "0.0.4"}}
   :main-opts ["-m" "spartan.test" "-n" "borkdude.deps-test"]}}}
```

you can invoke [`bb`](https://github.com/borkdude/babashka/) like this:

``` shell
$ deps.clj -A:test -Scommand "bb -cp {{classpath}} {{main-opts}}"
Ran 3 tests containing 3 assertions.
0 failures, 0 errors.
```

If you use `-Scommand` often, an alias can be helpful:

``` shell
$ alias babashka='rlwrap deps.clj -Scommand "bb -cp {{classpath}} {{main-opts}}"'
$ babashka -A:test
Ran 3 tests containing 3 assertions.
0 failures, 0 errors.
```

Additional args are passed along to the command:

``` shell
$ babashka -e '(+ 1 2 3)'
6
```

Note that we included `rlwrap` for a better REPL experience. Of course you can create another alias without `rlwrap` for CI:

``` shell
$ alias bbk='deps.clj -Scommand "bb -cp {{classpath}} {{main-opts}}"'
```

This approach can also be used with [planck](https://github.com/planck-repl/planck) or
[lumo](https://github.com/anmonteiro/lumo):

``` shell
$ alias lm='deps.clj -Scommand "lumo -c {{classpath}} {{main-opts}}"'
$ lm -Sdeps '{:deps {medley {:mvn/version "1.2.0"}}}' -K \
  -e "(require '[medley.core :refer [index-by]]) (index-by :id [{:id 1} {:id 2}])"
{1 {:id 1}, 2 {:id 2}}
```

### -Sdeps-file

The  `-Sdeps-file` option may be used to load a different project file than `deps.edn`.

## License

Copyright © 2019 Michiel Borkent

Distributed under the EPL License. See LICENSE.

This project is based on code from
[clojure/brew-install](https://github.com/clojure/brew-install/) which is
licensed under the same EPL License.
