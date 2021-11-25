set DEPS_CLJ_TEST_ENV=babashka

bb -cp "src;test;resources" ^
   -e "(require '[clojure.test :as t] '[borkdude.deps-test])" ^
   -e "(let [{:keys [:fail :error]} (t/run-tests 'borkdude.deps-test)] (System/exit (+ fail error)))"
