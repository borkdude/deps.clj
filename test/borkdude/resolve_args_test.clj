(ns borkdude.resolve-args-test

  (:require
     [borkdude.deps :as deps]
     [clojure.test :as t :refer [deftest is testing]]))


(deftest java-arg-merging
  (is (= ["-Dfoo=foo bar"]
         (->
          {:mode :repl,
           :deps-data "{:aliases {:my-alias {:jvm-opts [\"-Dfoo=foo bar\"]}}}",
           :repl-aliases [":my-alias"]}

          deps/resolve-args

          :jvm-cache-opts)))

  (is (= ["-Dfoo=foo bar" "--add-modules" "jdk.incubator.foreign" "--enable-native-access=ALL-UNNAMED"]
         (->
          {:mode :repl
           :deps-data (str
                       {:aliases
                        {:my-alias
                         {:jvm-opts ["-Dfoo=foo bar"]}
                         :jdk-17
                         {:jvm-opts ["--add-modules" "jdk.incubator.foreign"
                                     "--enable-native-access=ALL-UNNAMED"]}}})

           :repl-aliases [":my-alias" ":jdk-17"]}

          deps/resolve-args

          :jvm-cache-opts))))




(def test-classpath
  (is (= "src:resources:/home/carsten/.m2/repository/org/clojure/clojure/1.11.1/clojure-1.11.1.jar:/home/carsten/.m2/repository/org/clojure/core.specs.alpha/0.2.62/core.specs.alpha-0.2.62.jar:/home/carsten/.m2/repository/org/clojure/spec.alpha/0.3.218/spec.alpha-0.3.218.jar"

         (->
          {:mode :repl,
           :deps-data (str {:deps {'org.clojure/clojure  {:mvn/version "1.11.1" :scope "provided"}}})
           :repl-aliases [":my-alias"]}

          deps/resolve-args
          :classpath))))
