(ns wiki-graph.server
  (:require [wiki-graph.search :refer [execute-search]]
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

            [reitit.ring :as ring]

            [org.httpkit.server :as http-kit]

            [ring.util.response :refer [response bad-request not-found]]
            [ring.middleware.params :refer [wrap-params]]
            ))


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

(def QueryParams
  [:map
   ["start" :string]
   ["job_count" #"\d+" ]
   ["slide" #"true|false"]])

(m/=> read-input-config
      [:=>
       [:cat [:maybe [:map-of :string :string]]]
       [:maybe InputConfig]])

(defn read-input-config [params]
  (when (m/validate QueryParams params)

    (let [[initial-job job-count should-slide]
          (map params ["start" "job_count" "slide"])]

      {:initial-job initial-job
       :job-count (Integer/parseInt job-count)
       :should-slide (if (nil? should-slide) false (Boolean/parseBoolean should-slide))}

      )

    ))

(def Request :any)
(def Response :any)

(m/=> handle-search-request [:=> [:cat Request] Chan])

(defn handle-search-request [request]
  (go
    (let [input-config (read-input-config (:query-params request))
          start-term (:initial-job input-config)]

      (println "Request!")

      (cond

        (not input-config)
        (do (bad-request "Invalid query params."))

        (not
         (or (graph/get start-term)

             (<! (fetch/target-exists start-term))))
        (not-found (str "Unknown page " start-term))

        :else
        (http-kit/with-channel request channel
          (if (http-kit/websocket? channel)

            (<! (perform-search input-config channel))

            (http-kit/close channel)
            )
          ))

      ))


  )

(defn handle-stats-request [request]

  (let [counts (graph/list-counts)
        word-stats (stats/get-statistics-by (comp count first) counts)
        children-stats (stats/get-statistics-by second counts)
        json-output (json/write-str { :words word-stats :children children-stats })
        ]

    (ok json-output)

    )
  )

(def router
  (ring/router
   [["/stats" { :get handle-stats-request }]
    ["/search" { :get (comp <!! handle-search-request) }]] )
  )

(def app
  (-> (ring/ring-handler router)
      wrap-params
      wrap-access-control)
  )
