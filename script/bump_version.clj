#!/usr/bin/env bb

(ns bump-version
  (:require [babashka.process :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def version-file (io/file "resources" "DEPS_CLJ_VERSION"))
(def released-version-file (io/file "resources" "DEPS_CLJ_RELEASED_VERSION"))
(def src-file (io/file "src" "borkdude" "deps.clj"))
(def changelog-file (io/file "CHANGELOG.md"))

(def tag-re #"refs/tags/(\d+\.\d+\.\d+\.\d+)$")

(defn latest-brew-install-version []
  (let [{:keys [out]} (p/shell {:out :string}
                               "git" "ls-remote" "--tags" "--sort=-v:refname"
                               "https://github.com/clojure/brew-install")]
    (or (some (fn [line]
                (when-let [[_ v] (re-find tag-re line)]
                  v))
              (str/split-lines out))
        (throw (ex-info "No brew-install version tag found" {})))))

(defn update-src-version! [new-version]
  (let [src (slurp src-file)
        updated (str/replace
                 src
                 #"(\(System/getenv \"DEPS_CLJ_TOOLS_VERSION\"\)\s+\")\d+\.\d+\.\d+\.\d+(\")"
                 (str "$1" new-version "$2"))]
    (when (= src updated)
      (throw (ex-info "Failed to update version literal in src" {:file (str src-file)})))
    (spit src-file updated)))

(defn prepend-changelog! [new-version]
  (let [content (slurp changelog-file)
        marker "[deps.clj](https://github.com/borkdude/deps.clj): a faithful port of the clojure CLI bash script to Clojure\n"
        idx (str/index-of content marker)]
    (when-not idx
      (throw (ex-info "Could not find changelog marker in CHANGELOG.md" {})))
    (let [insert-at (+ idx (count marker))
          head (subs content 0 insert-at)
          tail (subs content insert-at)
          entry (format "\n## %s\n\n- Catch up with Clojure CLI %s\n"
                        new-version new-version)]
      (spit changelog-file (str head entry tail)))))

(defn release []
  (let [new-version (latest-brew-install-version)]
    (println "Latest brew-install version:" new-version)
    (spit version-file new-version)
    (update-src-version! new-version)
    (p/shell "script/gen_script.clj")
    (prepend-changelog! new-version)
    (p/shell "git" "commit" "-a" "-m" new-version)
    (p/shell "git" "diff" "HEAD^" "HEAD")))

(defn post-release []
  (io/copy version-file released-version-file)
  (let [version-string (str/trim (slurp version-file))
        numbers (str/split version-string #"\.")
        patch (last numbers)
        patch (str/replace patch "-SNAPSHOT" "")
        patch (Integer. patch)
        patch (str (inc patch) "-SNAPSHOT")
        new-version (str/join "." (concat (butlast numbers) [patch]))]
    (spit version-file new-version)
    (p/shell "script/gen_script.clj")
    (p/shell "git" "commit" "-a" "-m" "Version bump")
    (p/shell "git" "diff" "HEAD^" "HEAD")))

(when (= *file* (System/getProperty "babashka.file"))
  (case (first *command-line-args*)
    "release" (release)
    "post-release" (post-release)
    (println "Expected: release | post-release.")))
