(ns wiki-graph.core
  (:gen-class)
  (:require [wiki-graph.search :refer [execute-search]]
            [wiki-graph.reload :refer [reload]]
            [wiki-graph.statistics :as stats]
            [wiki-graph.fetch-refs :as fetch]
            [wiki-graph.graph :as graph]
            [wiki-graph.ring-util :refer [wrap-access-control ok]])

  (:require [clojure.core.async :as a :refer [>! <! >!! <!! go]]
            [clojure.data.json :as json]

            [org.httpkit.server :as http-kit]
            [ring.util.response :refer [response bad-request not-found]]

            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [GET routes]]
            ))


(def stop (atom (fn [])))

(defn r [] (@stop) (reload))

(defn on-successful-search-request [start-term config channel]
  (go
    (let [on-notify
          (fn [[parent item]]
            (http-kit/send! channel
                            (json/write-str {:parent parent
                                             :item item})))
          ]

      (<! (execute-search start-term config on-notify))

      (http-kit/close channel)

      ))
  )

(defn read-config [params]
  (when-not (nil? params)

    (let [[start-term task-count per-task channel-size should-slide]
          (map params ["start" "tasks" "per_task" "channel_size" "slide"])
          ]
      (and start-term task-count per-task
           {:start-term start-term
             :task-count (Integer/parseInt task-count)
             :per-task (Integer/parseInt per-task)
             :channel-size (if (nil? channel-size) nil (Integer/parseInt channel-size))
             :should-slide (if (nil? should-slide) false (Boolean/parseBoolean should-slide))})
      )

    )
  )

(defn handle-search-request [request]
  (let [config (read-config (:query-params request))
        start-term (:start-term config)]

    (cond
      (or (nil? start-term) (nil? config)) (bad-request "Missing parameters")

      (not
       (or (graph/get start-term)

           ;; TODO: improve this
           (<!! (fetch/target-exists start-term)))) (not-found (str "Unknown page " start-term))
      :else
      (http-kit/with-channel request channel
        (if (http-kit/websocket? channel)

          (on-successful-search-request start-term config channel)

          (http-kit/close channel)
          )
        ))

    )


  )

(def app-routes
  (routes
   (GET "/stats" []

        (let [counts (graph/list-counts)
              word-stats (stats/get-statistics-by (comp count first) counts)
              children-stats (stats/get-statistics-by second counts)
              json-output (json/write-str { :words word-stats :children children-stats })
              ]

          (ok json-output)

          ))

   ;; TODO: use compojure-api

   (GET "/search" request
        (handle-search-request request))
   )
  )

(def app
  (-> app-routes
      wrap-params
      wrap-access-control)
  )



(defn -main []
  (reset! stop (http-kit/run-server app { :port 8001 })))
