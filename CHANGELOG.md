# Changelog

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

- Add config-dir to -Sdescribe #38
- Catch up with Clojure CLI version 1.10.3.855
- Support for resolving from a different directory, for babashka tasks

## v0.0.15

Passing -J options doesn't work correctly #46

## v0.0.14

- Fix issue on Windows with spaces #43
