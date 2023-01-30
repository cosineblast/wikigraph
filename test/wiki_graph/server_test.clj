(ns wiki-graph.server-test
  (:require  [clojure.test :as t :refer :all]
             [wiki-graph.core :refer [app]]
             [ring.mock.request :as mock]))


(deftest test-stats
  (let [request (mock/request :get "/stats")
        result (app request)]

    (is (= 200 (:status result)))

    ;; todo: add request and response spec
    ))

(deftest test-search
  (let [request (mock/request :get "/search?start=Clojure&tasks=2&per_task=10&channel_size=100&slide=false")
        result (app request)]

    (is (= 200 (:status result)))

    ;; todo: add request and response spec
    ))
