(ns wiki-graph.core
  (:gen-class)
  (:require [wiki-graph.search :refer [execute-search]]
            [wiki-graph.reload :refer [reload]])

  (:require [clojure.core.async :as a :refer [>! <! >!! <!! go]]

            [org.httpkit.server :as httpkit]

            [ring.util.response :refer [response]]
            [ring.middleware.params :refer [wrap-params]]

            [compojure.core :refer [GET routes]]
            ))


(def stop (atom (fn [])))

(defn r [] (@stop) (reload))

(def app-routes
  (routes
   (GET "/stats" []

        (response "{}")
        )

   (GET "/search" request

        (let [params (:query-params request)

              [task-count todo-count channel-size]

              (map (comp #(Integer/parseInt %) params)
                   ["tasks" "per_task" "channel_size"])
              ]

          (response (str (+ task-count todo-count channel-size)))

          ))

   )
  )

(def app
  (-> app-routes
      wrap-params)
  )



(defn -main []
  (reset! stop (httpkit/run-server app { :port 8000 })))
