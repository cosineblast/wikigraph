(ns wiki-graph.util-test
  (:require [wiki-graph.util :as sut :refer [offer-onto-chan]]
            [clojure.test :refer :all]
            [clojure.core.async :as a :refer [>! >!! <! <!!]]))

(defn or-timeout
  ([time channel]
   (or-timeout time channel :timeout))

  ([time channel on-timeout]
   (a/go
     (a/alt!
       (a/timeout time) on-timeout
       channel ([x _] x)
       )
     )
   ))

(deftest offer-onto-chan-succeeds
  (testing "offer-onto-chan returns true channel when it succeeds."

    (let [k (a/chan 3)
          result (offer-onto-chan k [10 20 30])]

      (is (= true result))

      (is (= [10 20 30]
             (<!! (or-timeout 100 (a/into [] k)))
             ))

      )
    )
  )

(deftest offer-onto-chan-fails
  (testing "offer-onto-chan returns false when it fails."

    (let [k (a/chan 3)
          result (offer-onto-chan k [10 20 30 40])]

      (is (= false result))

      (is (= [10 20 30]
             (<!! (or-timeout 100 (a/into [] k)))
             ))

      )
    )
  )
