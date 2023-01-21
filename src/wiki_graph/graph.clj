(ns wiki-graph.graph
  (:refer-clojure :exclude [get assoc])
  (:require [taoensso.carmine :as car :refer [wcar]]))

(defonce redis-pool (car/connection-pool {}))

(def redis-spec { :uri "redis://127.0.0.1:6379" })

(def carmine-options
  {:pool redis-pool
   :spec redis-spec
   })

(defmacro wcar* [& body] `(wcar carmine-options ~@body))

(def result-graph (atom {}))

(defn get [job]
  (wcar* (car/get (str "wiki:" job)))
  )

(defn assoc [job refs]
  (wcar* (car/set (str "wiki:" job) refs)))
