(ns wiki-graph.core-test
  (:require [clojure.test :refer :all]
            [wiki-graph.core :refer :all]
            [wiki-graph.fetch :refer [fetch-wiki-refs-async]]
            [wiki-graph.graph :as graph]
            [clojure.core.async :as a :refer [<!!]]
            )
  )


(deftest a
  (is true))
