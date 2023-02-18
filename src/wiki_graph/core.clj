(ns wiki-graph.core
  (:gen-class)

  (:require [wiki-graph.server :as server]
            [org.httpkit.server :as http-kit]
            ))


(defonce quit (atom (fn [])))

(defn r
  ([namespace]
   (@quit)
   (require namespace :reload)))



(defn -main []
  (@quit)
  (reset! quit (http-kit/run-server server/app { :port 8001 })))
