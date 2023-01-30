(ns wiki-graph.fetch-test
  (:require [clojure.test :as t :refer :all]
            [wiki-graph.core :refer :all]
            [wiki-graph.fetch :refer [fetch-wiki-refs-async]]
            [wiki-graph.graph :as graph]
            [clojure.core.async :as a :refer [<!!]]
            ))

;; TODO: write tests for wiki-graph.fetch/target-exists

(deftest fetch-async-succeeds
  (testing "Fetches a page successfully."

    (let [channel (wiki-graph.fetch/fetch-wiki-refs-async "Communicating_sequential_processes")
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

    (let [channel (wiki-graph.fetch/fetch-wiki-refs-async
                   "ThisThingyDoesNOTExist")

          result (<!! channel)]

      (is (not (:value result)))

      (is (:error result))

      (is (= (:error result) 404))

      )
    )
  )
