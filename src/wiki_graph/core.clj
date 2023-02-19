(ns wiki-graph.core
  (:gen-class)

  (:require [wiki-graph.server :as server]
            [org.httpkit.server :as http-kit]
            [malli.dev]
            ))

(declare quit)

(defonce quit (atom (fn [])))

(defn r
  ([namespace]
   (@quit)
   (require namespace :reload)))

(defn -main []
  (@quit)
  (reset! quit (http-kit/run-server server/app { :port 8004 })))

(malli.dev/start!)
