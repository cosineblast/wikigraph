(ns wiki-graph.core
  (:gen-class)
  (:require [wiki-graph.search :refer [execute-search]])
  (:require [clojure.core.async :as a :refer [>! <! >!! <!! go]]))

(defn -main []
  (execute-search { :thread-count 2 :todo-count 10 :channel-size 3000 }))
