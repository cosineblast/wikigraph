(ns wiki-graph.server
  (:require [wiki-graph.search :refer [execute-search]]
            [wiki-graph.statistics :as stats]
            [wiki-graph.fetch :as fetch]
            [wiki-graph.graph :as graph]
            [wiki-graph.ring-util :refer [wrap-access-control ok]]
            [wiki-graph.util :refer [Chan Deferred]]

            [wiki-graph.search :as search])

  (:require [clojure.data.json :as json]

            [malli.dev :as dev]
            [malli.core :as m]

            [reitit.ring :as ring]

            [org.httpkit.server :as http-kit]

            [ring.util.response :refer [response bad-request not-found]]
            [ring.middleware.params :refer [wrap-params]]

            [muuntaja.middleware]

            [manifold.deferred :as d :refer [let-flow]]
            ))

(defn deferred-of [x]
  (let [result (d/deferred)]
    (d/success! result x)
    result
    )
  )


(def InputConfig :any)


(m/=> perform-search [:=> [:cat InputConfig :any] Deferred])

(defn perform-search [input-config channel]
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

    (let-flow [_ (execute-search config on-notify)]
      (http-kit/close channel)
      )
    )
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

(defn page-exists [page]
  (if (graph/get page)
    (deferred-of true)
    (fetch/target-exists-deferred page)
    ))


(defn handle-search-request [request]
  (let-flow [input-config (read-input-config (:query-params request))
             start-term (:initial-job input-config)]

    (println "Request!")

    (if-not input-config
      (bad-request "Invalid query params.")

      (let-flow [exists (page-exists start-term)]

        (if-not exists
          (not-found (str "Unknown page " start-term))

          (http-kit/with-channel request channel
            (if (http-kit/websocket? channel)

              (perform-search input-config channel)

              (http-kit/close channel)
              )
            )
          )
        )
      )
    ))

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
   [["/stats"
     {:get handle-stats-request
      :middleware [muuntaja.middleware/wrap-format]}]

    ["/search" {:get (comp deref handle-search-request)}]] )
  )

(def app
  (-> (ring/ring-handler router)
      wrap-params
      wrap-access-control)
  )
