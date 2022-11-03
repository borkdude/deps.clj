(ns artifact
  (:require [borkdude.gh-release-artifact :as ghr]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]))

(defn current-branch []
  (or (System/getenv "APPVEYOR_PULL_REQUEST_HEAD_REPO_BRANCH")
      (System/getenv "APPVEYOR_REPO_BRANCH")
      (System/getenv "CIRCLE_BRANCH")
      (System/getenv "GITHUB_REF_NAME")
      (System/getenv "CIRRUS_BRANCH")
      (-> (sh "git" "rev-parse" "--abbrev-ref" "HEAD")
          :out
          str/trim)))

(defn upload [opts]
  (assert (:file opts) "File is required")
  (let [ght (System/getenv "GITHUB_TOKEN")
        current-version
        (-> (slurp "resources/DEPS_CLJ_VERSION")
            str/trim)
        branch (current-branch)]
    (if (and ght (contains? #{"master" "main"} branch))
      (ghr/release-artifact (merge {:org "borkdude"
                                    :repo "deps.clj"
                                    :tag (str "v" current-version)
                                    :file "README.md"
                                    :sha256 true}
                                   opts))
      (println "Skipping release artifact (no GITHUB_TOKEN or not on main branch)"))))
