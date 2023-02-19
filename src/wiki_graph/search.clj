(ns wiki-graph.search
  (:require [clojure.core.async :as a :refer [>! <! go]]
            [malli.core :as m]
            )

  (:require [wiki-graph.graph :as graph]
            [wiki-graph.fetch :refer [fetch-wiki-refs-async]]
            [wiki-graph.report :refer [report with-thread-name]]
            [wiki-graph.util :as util :refer [Chan]]

            [manifold.deferred :as d]
            )
  )


(def Config
  [:map { :closed true }
   [:initial-job :string]
   [:job-count :int]
   [:task-count :int]
   [:pending-limit :int]
   [:should-slide :boolean]])


(def ^:dynamic *is-halted*)
(def ^:dynamic *seen-refs*)
(def ^:dynamic *todo-count*)
(def ^:dynamic *notify*)

(def Job :string)

(m/=> get-refs [:=> [:cat Chan] Chan])

(defn get-refs [job-channel]

  (a/go

    (when-let [[parent job] (<! job-channel)]

      (report "Job" job "aquired! Getting Refs...")

      (if-let [cached (graph/get job)]

        (do (report "Job was cached!") [parent job cached])

        (let [{:keys [error value]} (<! (fetch-wiki-refs-async job))]
          (if error
            (do (report "Error while fetching refs:" error) nil)

            [parent job value]
            )

          ))

      ))
  )

(m/=> push-refs
      [:=> [:cat Chan Job [:set Job]] Chan])

(defn- push-refs [job-channel job input-refs]
  (a/go

    (graph/assoc job input-refs)
    (swap! *seen-refs* conj job)

    (let [stuff (for [ref input-refs :when (not (@*seen-refs* ref))] [job ref])]

      (util/offer-onto-chan job-channel stuff false)
      )

    ))

(m/=> halt! [:=> [:cat Chan] :nil])

(defn- halt! [job-channel]
  (report "HALTING")
  (reset! *is-halted* true)
  (a/close! job-channel)
  )

(m/=> run-fetcher [:=> [:cat Chan] Chan])

(defn- run-fetcher [job-channel]
  (a/go
    (loop [i 0]
      (report "Fetcher Starting")

      (when (and (>= (swap! *todo-count* dec) 0) (not @*is-halted*))

        (report i "jobs done.")
        (report (str "Aquiring job... "))

        (if-let [[parent job refs] (<! (get-refs job-channel))]

          (do
            (report "Refs Fetched! Putting refs...")

            (let [is-success (<! (push-refs job-channel job refs))]

              (if is-success
                (do (report "Job Done!")
                    (*notify* [parent job])
                    (recur (inc i)))

                (do (report "Push Failed") (halt! job-channel))
                ))
            )

          (halt! job-channel)
          )))

    (report "Fetcher finished!")
    ))


(m/=> start-fetchers [:=> [:cat Config] [:tuple :any :any]])

(defn- start-fetchers [config]
  (let [thread-count (:task-count config)

        channel-size (:pending-limit config)
        buffer-type (if (:should-slide config) a/sliding-buffer a/buffer)
        job-channel (a/chan (buffer-type channel-size))

        threads
        (for [i (range thread-count)]
          (with-thread-name (str "F" i)
            (run-fetcher job-channel))
          )]

    [(doall threads) job-channel])
  )


(m/=> execute-search-async [:=> [:cat Config fn?] Chan])

(defn- execute-search-async [config on-notification]

  (a/go
    (report "Search Starting")

    (binding [*is-halted* (atom false)
              *seen-refs* (atom #{})
              *todo-count* (atom (:job-count config))
              *notify* on-notification]

      (let [[threads job-channel] (start-fetchers config)
            initial-job (:initial-job config)]

        (report "Adding Job:" initial-job)

        (>! job-channel [nil initial-job])

        (report "Job Added")

        (doseq [thread threads] (<! thread))

        (a/close! job-channel)

        (report "Done!")

        (not @*is-halted*))

      )))

(defn execute-search [config on-notification]

  (let [result (d/deferred)]

    (go (d/success! result (<! (execute-search-async config on-notification))))

    result)

  )
