(ns wiki-graph.core
  (:gen-class)
  (:require [wiki-graph.fetch-refs :refer :all]
            [wiki-graph.report :refer :all]
            [wiki-graph.statistics :as stats]
            [wiki-graph.reload :refer [r]]
            [wiki-graph.timing :refer [timing time!]]
            [wiki-graph.graph :as graph]
            [wiki-graph.search :refer [execute-search]]
            )

  (:require [clojure.core.async :as a :refer [>! <! >!! <!! go]]))

(defn -main []
  (execute-search))
