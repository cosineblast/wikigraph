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
            [reitit.coercion.malli]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware.exception]

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

(defn parameters-to-config [parameters]
  {:initial-job (:start parameters)
   :job-count (:job_count parameters)
   :should-slide (:slide parameters)
   :task-count 4
   :pending-limit 100000

   })

(defn perform-search [parameters channel]
  (let [config (parameters-to-config parameters)

        on-notify
        (fn [[parent item]]
          (http-kit/send!
           channel (json/write-str {:parent parent :item item})))

        ]

    (let-flow [_ (execute-search config on-notify)]
      (http-kit/close channel)
      )
    )
  )

(def QueryParams
  [:map
   [:start :string]
   [:job_count :int]
   [:slide :boolean]])

(def Request :any)
(def Response :any)

(defn page-exists [page]
  (if (graph/get page)
    (deferred-of true)
    (fetch/target-exists-deferred page)
    ))


(defn handle-search-request [request]
  (let-flow [parameters (-> request :parameters :query)
             start-term (:start parameters)]

    (let-flow [exists (page-exists start-term)]

      (if-not exists
        (not-found (str "Unknown page " start-term))

        (http-kit/with-channel request channel
          (if (http-kit/websocket? channel)

            (perform-search parameters channel)

            (http-kit/close channel)
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
      :middleware [muuntaja.middleware/wrap-format]

      }]

    ["/search"
     {:get (comp deref handle-search-request)

      :parameters {:query QueryParams}}
     ]]

   {:data
    {:coercion reitit.coercion.malli/coercion
     :middleware [reitit.ring.middleware.exception/exception-middleware
                  wrap-access-control
                  wrap-params
                  rrc/coerce-request-middleware

                  ]}
    })
  )


(def app
  (ring/ring-handler
   router
   )
  )
