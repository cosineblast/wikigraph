(ns wiki-graph.graph
  (:refer-clojure :exclude [get assoc])
  (:require [taoensso.carmine :as car :refer [wcar]]
            [malli.core :as m]))

(defonce redis-pool (car/connection-pool {}))

(def redis-spec { :uri "redis://127.0.0.1:6379" })

(def carmine-options
  {:pool redis-pool
   :spec redis-spec
   })

(defmacro wcar* [& body] `(wcar carmine-options ~@body))

(def result-graph (atom {}))


(m/=> get [:=> [:cat :string] [:set :string]])

(defn get [job]
  (wcar* (car/get (str "wiki:" job)))
  )

(m/=> assoc [:=> [:cat :string [:set :string]] :nil])

(defn assoc [job refs]
  (wcar* (car/set (str "wiki:" job) refs))
  nil)

(m/=> list-counts [:=> [:cat] [:sequential [:tuple :string :int]]])

(defn list-counts []
  (let [keys (wcar* (car/keys "wiki:*"))
        values (mapv count (wcar* :as-pipeline (doall (map car/get keys))))]

    (map vector (map #(subs % 5) keys) values)
    ))
