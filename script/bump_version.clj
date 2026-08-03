#!/usr/bin/env bb

(ns bump-version
  (:require [babashka.fs :as fs]
            [babashka.http-client :as http]
            [babashka.process :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def version-file (io/file "resources" "DEPS_CLJ_VERSION"))
(def released-version-file (io/file "resources" "DEPS_CLJ_RELEASED_VERSION"))
(def src-file (io/file "src" "borkdude" "deps.clj"))
(def changelog-file (io/file "CHANGELOG.md"))

(def tag-re #"refs/tags/(\d+\.\d+\.\d+\.\d+)$")

(def tools-version-re
  #"(\(System/getenv \"DEPS_CLJ_TOOLS_VERSION\"\)\s+\")(\d+\.\d+\.\d+\.\d+)(\")")

(defn latest-brew-install-version []
  (let [{:keys [out]} (p/shell {:out :string}
                               "git" "ls-remote" "--tags" "--sort=-v:refname"
                               "https://github.com/clojure/brew-install")]
    (or (some (fn [line]
                (when-let [[_ v] (re-find tag-re line)]
                  v))
              (str/split-lines out))
        (throw (ex-info "No brew-install version tag found" {})))))

(defn current-tools-version []
  (or (nth (re-find tools-version-re (slurp src-file)) 2 nil)
      (throw (ex-info "Could not find tools version literal in src"
                      {:file (str src-file)}))))

(defn install-script
  "The upstream `clojure` install script at brew-install tag `version`."
  [version]
  (let [url (format "https://raw.githubusercontent.com/clojure/brew-install/%s/src/main/resources/clojure/install/clojure"
                    version)
        {:keys [status body]} (http/get url {:throw false})]
    (when-not (= 200 status)
      (throw (ex-info (str "Could not fetch install script for " version)
                      {:url url :status status})))
    body))

(defn check-install-script!
  "Exit when the upstream install script changed between the two versions:
  those changes need to be ported into src by hand."
  [old-version new-version]
  (let [old-script (install-script old-version)
        new-script (install-script new-version)]
    (if (= old-script new-script)
      (println "Install script unchanged between" old-version "and" new-version)
      (let [dir (fs/create-temp-dir {:prefix "deps-clj-bump"})
            old-file (fs/file dir (str "clojure-" old-version))
            new-file (fs/file dir (str "clojure-" new-version))]
        (spit old-file old-script)
        (spit new-file new-script)
        (println "Install script CHANGED between" old-version "and" new-version)
        (println)
        (p/shell {:continue true} "diff" "-u" (str old-file) (str new-file))
        (println)
        (println "Port these changes into" (str src-file)
                 "first, then rerun with --force.")
        (System/exit 1)))))

(defn update-src-version! [new-version]
  (let [src (slurp src-file)
        updated (str/replace
                 src
                 tools-version-re
                 (str "$1" new-version "$3"))]
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

(defn release [{:keys [force?]}]
  (let [old-version (current-tools-version)
        new-version (latest-brew-install-version)]
    (println "Current tools version:" old-version)
    (println "Latest brew-install version:" new-version)
    (when (= old-version new-version)
      (println "Already up to date.")
      (System/exit 0))
    (if force?
      (println "Skipping install script check (--force)")
      (check-install-script! old-version new-version))
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

(let [[command & args] *command-line-args*
      opts {:force? (contains? (set args) "--force")}]
  (case command
    "release" (release opts)
    "post-release" (post-release)
    (println "Expected: release [--force] | post-release.")))
