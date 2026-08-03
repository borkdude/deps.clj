# deps.clj

Port of the Clojure CLI bash `clojure` install script to Clojure. Mirrors `clojure/brew-install` versions.

## Bumping to a new Clojure CLI version

1. Find latest `brew-install` version: tags at https://github.com/clojure/brew-install/tags (or `git ls-remote --tags --sort=-v:refname https://github.com/clojure/brew-install`).
2. Verify the install script did not change between current and latest:
   ```
   curl -sL "https://raw.githubusercontent.com/clojure/brew-install/<CUR>/src/main/resources/clojure/install/clojure" -o cur.sh
   curl -sL "https://raw.githubusercontent.com/clojure/brew-install/<NEW>/src/main/resources/clojure/install/clojure" -o new.sh
   diff cur.sh new.sh
   ```
   Empty diff = no port changes needed. Non-empty = port the changes into `src/borkdude/deps.clj`.
3. Bump version in three places:
   - `src/borkdude/deps.clj`: the `DEPS_CLJ_TOOLS_VERSION` fallback literal.
   - `resources/DEPS_CLJ_VERSION`.
   - `CHANGELOG.md`: prepend `## <NEW>` entry with `- Catch up with Clojure CLI <NEW>`.
4. Regenerate root `deps.clj` + `deps.bat` from the src:
   ```
   bb gen-script
   ```
5. Review `git diff`. Only the tools version literal should change in the generated scripts.

## Automated path

`bb bump-version release` does all of the above (fetch latest, diff the upstream install script, bump files, gen-script, changelog) AND commits. Use the manual steps when you do not want an auto-commit.

It stops with exit code 1 and prints the diff when the install script changed between the current and the latest version: those changes need to be ported into `src/borkdude/deps.clj` by hand. Rerun as `bb bump-version release --force` to bump the version numbers after porting.

`bb bump-version post-release` copies `DEPS_CLJ_VERSION` to `DEPS_CLJ_RELEASED_VERSION` and bumps to the next `-SNAPSHOT`.

## Files

- `src/borkdude/deps.clj` - source of truth.
- `deps.clj` / `deps.bat` - generated, do not edit directly.
- `resources/DEPS_CLJ_VERSION` - current (SNAPSHOT between releases).
- `resources/DEPS_CLJ_RELEASED_VERSION` - last released.
- `script/gen_script.clj`, `script/bump_version.clj` - tooling.
