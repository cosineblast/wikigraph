(ns wiki-graph.util
  (:require [malli.core :as m])
  (:import [clojure.core.async.impl.channels ManyToManyChannel])
  )

(m/=> chan? [:=> [:cat :any] :boolean])

(defn chan? [x]
  (instance? ManyToManyChannel x))

(def Chan [:fn
           {:title "Chan"
            :description "A clojure async ManyToManyChannel."}
           chan?])
