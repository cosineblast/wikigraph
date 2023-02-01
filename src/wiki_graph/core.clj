(ns wiki-graph.core
  (:gen-class)
  (:require [wiki-graph.search :refer [execute-search]]
            [wiki-graph.reload :refer [reload]]
            [wiki-graph.statistics :as stats]
            [wiki-graph.fetch :as fetch]
            [wiki-graph.graph :as graph]
            [wiki-graph.ring-util :refer [wrap-access-control ok]]
            [wiki-graph.util :refer [Chan]]

            [wiki-graph.search :as search])

  (:require [clojure.core.async :as a :refer [>! <! >!! <!! go]]
            [clojure.data.json :as json]

            [malli.dev :as dev]
            [malli.core :as m]
            [malli.dev.pretty :as pretty]

            [org.httpkit.server :as http-kit]
            [ring.util.response :refer [response bad-request not-found]]

            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [GET routes]]
            ))


(def stop (atom (fn [])))

(defn r [] (@stop) (reload))

(def InputConfig :any)


(m/=> perform-search [:=> [:cat InputConfig Chan] Chan])

(defn perform-search [input-config channel]
  (go
    (let [config (into input-config
                       {:task-count 4
                        :pending-limit 100000
                        })

          on-notify
          (fn [[parent item]]
            (http-kit/send! channel
                            (json/write-str {:parent parent
                                             :item item})))
          ]

      (<! (execute-search config on-notify))

      (http-kit/close channel)

      ))
  )

(def QueryParams :any)

(m/=> read-input-config
      [:=>
       [:cat [:maybe QueryParams]]
       [:maybe InputConfig]])

(defn read-input-config [params]
  (when-not (nil? params)

    (let [[initial-job job-count should-slide]
          (map params ["start" "job_count" "slide"])]

      {:initial-job initial-job
       :job-count (Integer/parseInt job-count)
       :should-slide (if (nil? should-slide) false (Boolean/parseBoolean should-slide))}

      )

    ))

(def Request :any)
(def Response :any)

(m/=> handle-search-request [:=> [:cat Request] Response])

(defn handle-search-request [request]
  (let [input-config (read-input-config (:query-params request))
        start-term (:initial-job input-config)]

    (cond
      (or (not start-term) (not input-config)) (do (println "UHH") (bad-request "Missing parameters"))

      (not
       (or (graph/get start-term)

           ;; TODO: improve this
           (<!! (fetch/target-exists start-term)))) (not-found (str "Unknown page " start-term))
      :else
      (http-kit/with-channel request channel
        (if (http-kit/websocket? channel)

          (perform-search input-config channel)

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
