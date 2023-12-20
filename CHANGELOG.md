# Changelog

Many releases just bump the clojure tools version and do not have new
features. You can also accomplish that via the `DEPS_CLJ_TOOLS_VERSION`
environment variable:

```
DEPS_CLJ_TOOLS_VERSION=1.11.1.1165 bb clojure
```

[deps.clj](https://github.com/borkdude/deps.clj): a faithful port of the clojure CLI bash script to Clojure

## Unreleased

- Support older CPUs for native-image builds

## 1.11.1.1429

- Catch up with CLI `1.11.1.1429`
- [#114](https://github.com/borkdude/deps.clj/issues/114): Align with original scripts on how to determine home directory

## 1.11.1.1413

- Catch up with CLI `1.11.1.1413`
- [#113](https://github.com/borkdude/deps.clj/issues/113): Fix bug in SHA comparison of downloaded tools jar with leading zeroes

## 1.11.1.1403

- Verify downloaded zip file with .sha256 file

## 1.11.1.1386

- [#104](https://github.com/borkdude/deps.clj/issues/104): print repl-aliases in -Sdescribe
- Make installation of tools jar more robust using transaction file
- [#106](https://github.com/borkdude/deps.clj/issues/106): System deps.edn should be extracted on tools install
- Download tools jar from new Github releases link

## 1.11.1.1347

- Fix stale check for `.jar` files when `*dir*` is set
- Expose `*getenv-fn*` for programmatic API usage
- Catch up with CLI `1.11.1.1347`

## 1.11.1.1273-4

- Expose [API.md](API.md)
- Automatically use file argument for classpath when command line arguments exceeds supported length on Windows.
  See [this article](https://devblogs.microsoft.com/oldnewthing/20031210-00/?p=41553) for more info.

## 1.11.1.1273

- Catch up with clojure CLI of same version

## 1.11.1.1267

- Catch up with clojure CLI of same version

## 1.11.1.1262

- Catch up with clojure CLI of same version

## 1.11.1.1257

- Catch up with clojure CLI of same version

## 1.11.1.1252

- Catch up with clojure CLI of same version

## 1.11.1.1237

- Catch up with clojure CLI 1.11.1.1237

## 1.11.1.1224

- Catch up with clojure CLI 1.11.1.1224

## v1.11.1.1208

- Catch up with clojure CLI 1.11.1.1208

## v1.11.1.1200

- Catch up with clojure CLI 1.11.1.1200

## v1.11.1.1189

- Catch up with clojure CLI 1.11.1.1189

## v1.11.1.1182

- [#71](https://github.com/borkdude/deps.clj/issues/71): Port stale check for jar files ([commit](https://github.com/clojure/brew-install/commit/f791abf1d93563c1ed8f256830bd0bfc085fdd53)]
- Bump tools jar to `1.11.1.1182`
- [#66](https://github.com/borkdude/deps.clj/issues/66): Respect `CLJ_JVM_OPTS` while downloading tools zip file ([@ikappaki](https://github.com/ikappaki))
- More tests in CI tests for various JVM versions and running all tests for Windows ([@ikappaki](https://github.com/ikappaki))

## v1.11.1.1165

- Add support for `CLJ_JVM_OPTS` and `JAVA_OPTS` environment variables ([@ikappaki](https://github.com/ikappaki))

## v0.1.1165

- Use tools version `1.11.1.1165`

## v0.1.1155-2

- Fix installation of `tools/tools.edn`

## v0.1.1155

- Use tools version `1.11.0.1155`

## v0.1.1100

- Use tools version `1.11.0.1100`

## v0.1.1087

- Use tools version `1.10.3.1087`

## v0.0.22

- Use tools version `1.10.3.1069`

## v0.0.21

- Fix for `DEPS_CLJ_TOOLS_VERSION`: delay reading to runtime in binary

## v0.0.20

- Use tools version `1.10.3.998`
- Add new `DEPS_CLJ_TOOLS_VERSION` environment variable to control tools version
- Renamed `CLOJURE_TOOLS_DIR` to `DEPS_CLJ_TOOLS_DIR` while preserving backwards compatibility

## v0.0.19

- Catch up with Clojure CLI version 1.10.3.986

## v0.0.18

- Catch up with Clojure CLI version 1.10.3.981

## v0.0.17

- Catch up with Clojure CLI version 1.10.3.967

## v0.0.16

- Add config-dir to -Sdescribe [#38](https://github.com/borkdude/deps.clj/issues/38)
- Catch up with Clojure CLI version 1.10.3.855
- Support for resolving from a different directory, for babashka tasks

## v0.0.15

Passing -J options doesn't work correctly [#46](https://github.com/borkdude/deps.clj/issues/46)

## v0.0.14

- Fix issue on Windows with spaces [#43](https://github.com/borkdude/deps.clj/issues/43)
