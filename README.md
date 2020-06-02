# deps.clj

[![CircleCI](https://circleci.com/gh/borkdude/deps.clj/tree/master.svg?style=shield)](https://circleci.com/gh/borkdude/deps.clj/tree/master)
[![Build status](https://ci.appveyor.com/api/projects/status/wwfs4utm08dd9vx2/branch/master?svg=true)](https://ci.appveyor.com/project/borkdude/deps.clj/branch/master)
[![Clojars Project](https://img.shields.io/clojars/v/borkdude/deps.clj.svg)](https://clojars.org/borkdude/deps.clj)

Clojure provides the [`clojure`](https://clojure.org/guides/deps_and_cli)
command line tool for:

- Running an interactive REPL (Read-Eval-Print Loop)
- Running Clojure programs
- Evaluating Clojure expressions

The `clojure` CLI is written in bash. This is a port of that tool written in
Clojure itself.

Features:

- Available as executable compiled with [GraalVM](https://github.com/oracle/graal)
- Run directly from source with [babashka](https://github.com/borkdude/babashka/) or the JVM
- Similar startup to bash
- Easy installation on all three major platforms including Windows
- Works in `cmd.exe` on Windows

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
  ported to Clojure successfully and runs just as fast with `bb`.

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
- as a JVM library or uberjar (see [Github
releases](https://github.com/borkdude/deps.clj/releases)).

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

## Extra features

The `deps.clj` script adds the following features compared to the `clojure`
tool:

```
 -Sdeps-file    Use this file instead of deps.edn
 -Scommand      A custom command that will be invoked. Substitutions: {{classpath}}, {{main-opts}}.
```

It also is able to pick up [proxy information from environment
variables](#proxy-environment-variables).

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
$ alias bbk='rlwrap deps.clj -Scommand "bb -cp {{classpath}} {{main-opts}}"'
$ bbk -A:test
Ran 3 tests containing 3 assertions.
0 failures, 0 errors.
```

The `bbk` alias is similar to the `clj` alias in that it adds `rlwrap`.

Additional args are passed along to the command:

``` shell
$ bbk -e '(+ 1 2 3)'
6
```

Of course you can create another alias without `rlwrap` for CI, similar to the
`clojure` command:

``` shell
$ alias babashka='deps.clj -Scommand "bb -cp {{classpath}} {{main-opts}}"'
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

### Proxy environment variables

deps.clj supports setting a proxy server via the "standard" environment variables (the lowercase versions are tried first):
- `http_proxy` or `HTTP_PROXY` for http traffic
- `https_proxy` or `HTTPs_PROXY` for https traffic

This will set the JVM properties `-Dhttp[s].proxyHost` and `-Dhttp[s].proxyPort`.

The format of the proxy string supported is `http[s]://[username:password@]host:port`. Any username and password info is ignored as not supported by the underlying JVM properties.

## Developing deps.clj

For running locally, you can invoke deps.clj with `clojure` (totally meta
right?). E.g. for creating a classpath with deps.clj, you can run:

```
$ clojure -m borkdude.deps -Spath
```

or with `lein`:

```
$ lein run -m borkdude.deps -Spath
```

To run jvm tests:

```
$ script/jvm_test
```

To run with babashka after making changes to `src/borkdude/deps.clj`, you should run:

```
$ script/gen_script.clj
```

and then:

```
$ ./deps.clj -Spath
# or
$ bb deps.clj -Spath
```

To run as an executable, you'll first have to compile it. First,
[download](https://github.com/graalvm/graalvm-ce-builds/releases) a GraalVM
distro. The compile script assumes that you will have set `GRAALVM_HOME` to the
location of your GraalVM installation. Currently this project uses `java-11-20.1.0`.

``` shell
$ export GRAALVM_HOME=/Users/borkdude/Downloads/graalvm-ce-java11-20.1.0/Contents/Home
```

The script also assumes that you have
[`lein`](https://github.com/technomancy/leiningen) installed.

Run the compile script with:

```
$ script/compile
```

If everything worked out, there will be a `deps.exe` binary in the root of the
project.

To run executable tests:

```
$ script/exe_test
```

## License

Copyright © 2019-2020 Michiel Borkent

Distributed under the EPL License. See LICENSE.

This project is based on code from
[clojure/brew-install](https://github.com/clojure/brew-install/) which is
licensed under the same EPL License.
