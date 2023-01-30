(ns wiki-graph.search-test
  (:require [wiki-graph.search :as sut]
            [clojure.test :as t :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]
            [clojure.core.async :refer [<!!]]))

(st/instrument)

(deftest search-succeeds
  (testing "Search succeeds without a halt on basic request"

    (let [done-count (atom 0)

          config {:initial-job "Clojure"
                  :task-count 2
                  :job-count 20
                  :pending-limit (* 2 10 200)
                  :should-slide false
                  }

          on-job (fn [_] (swap! done-count inc))

          success (<!! (sut/execute-search config on-job))
          ]

      (is success)

      (is (= 20 @done-count))
      )
    ))

(deftest search-halts
  (testing "Search finishes with a halt on small buffer size"

    (let [config {:initial-job "Clojure"
                  :task-count 2
                  :job-count 20
                  :pending-limit 20
                  :should-slide false
                  }
          success (<!! (sut/execute-search config identity))]

      (is (not success))
      )
    ))

(deftest search-slides
  (testing "Search finishes without a halt on small buffer size with sliding"

    (let [done-count (atom 0)

          config {:initial-job "Clojure"
                  :task-count 2
                  :job-count 30
                  :pending-limit 20
                  :should-slide true
                  }

          on-job (fn [_] (swap! done-count inc))

          success (<!! (sut/execute-search config identity))]

      (is success)

      (= done-count 60) ; (* task-count per-task)
      )
    ))
