(ns wiki-graph.core-test
  (:require [clojure.test :refer :all]
            [wiki-graph.core :refer :all]
            [wiki-graph.fetch-refs :refer [fetch-wiki-refs-async]]
            [wiki-graph.graph :as graph]
            [clojure.core.async :as a :refer [<!!]])
  )

;; TODO: write tests for wiki-graph.fetch-refs/target-exists

'(deftest graph-gets-sets

  (let [job "Thingy1"
        refs #{"Thingy2" "Thingy3" "Thingy4"}]

    (graph/assoc job refs)

    (is (graph/get job) refs)
    ))


(deftest fetch-async-succeeds
  (testing "Fetches a page successfully."

    (let [channel (wiki-graph.fetch-refs/fetch-wiki-refs-async "Communicating_sequential_processes")
          result (<!! channel)]

      (is (not (:error result)))

      (is (:value result))

      (let [refs (:value result)]
        (is (contains? refs "Go_(programming_language)")))
      )
    )
  )

(deftest fetch-async-reports-error
  (testing "Fetches a page successfully."

    (let [channel (wiki-graph.fetch-refs/fetch-wiki-refs-async
                   "ThisThingyDoesNOTExist")

          result (<!! channel)]

      (is (not (:value result)))

      (is (:error result))

      (is (= (:error result) 404))

      )
    )
  )
