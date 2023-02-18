(ns wiki-graph.server-test
  (:require  [clojure.test :as t :refer :all]
             [wiki-graph.server :refer [app]]
             [ring.mock.request :as mock]))


(deftest test-stats
  (let [request (mock/request :get "/stats")
        result (app request)]

    (is (= 200 (:status result)))

    ;; todo: add request and response spec
    ))
