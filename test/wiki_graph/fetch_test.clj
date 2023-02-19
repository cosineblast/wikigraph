(ns wiki-graph.fetch-test
  (:require [clojure.test :as t :refer [deftest testing is]]

            [wiki-graph.fetch :refer
             [target-exists fetch-wiki-refs-async]]

            [clojure.core.async :as a :refer [<!!]]
            ))

(def non-existent-page "__ThisWikipediaPageDoesNotExist__")

(deftest target-exists-reports-non-existent
  (testing "target-exists reports non-existent-pages."

    (is (not (<!! (target-exists non-existent-page))))

    ))

(deftest fetch-async-succeeds
  (testing "fetch-wiki-refs-async fetches pages."

    (let [result (<!! (fetch-wiki-refs-async "Communicating_sequential_processes"))]

      (is (not (:error result)))

      (is (:value result))

      (let [refs (:value result)]
        (is (contains? refs "Go_(programming_language)")))
      )
    )
  )

(deftest fetch-async-reports-error
  (testing "fetch-wiki-refs-async reports errors."

    (let [result (<!! (fetch-wiki-refs-async non-existent-page))]

      (is (not (:value result)))

      (is (:error result))

      (is (= (:error result) 404))

      )
    )
  )
