(ns wiki-graph.search
  (:require [clojure.core.async :as a :refer [>! >!! <! <!!]])

  (:require [wiki-graph.graph :as graph]
            [wiki-graph.fetch-refs :refer [fetch-wiki-refs-async]]
            [wiki-graph.report :refer [report with-thread-name]]
            ))


(def ^:dynamic *is-halted*)
(def ^:dynamic *seen-refs*)
(def ^:dynamic *notify*)

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

(defn push-refs [job-channel job input-refs]
  (a/go

    (graph/assoc job input-refs)
    (swap! *seen-refs* conj job)

    (loop [refs (filter (comp not @*seen-refs*) input-refs)]

      (if-let [[ref & more] (seq refs)]
        (if (>! job-channel [job ref])
          (recur more)
          :closed)
        :success
        ))

    ))

(defn halt! [job-channel]
  (report "HALTING")
  (reset! *is-halted* true)
  (a/close! job-channel)
  )

(defn result-or-timeout [channel time]
  (a/go
    (first (a/alts! [channel
                     (a/go (<! (a/timeout time)) :timeout)
                     ]))
    ))

(defn run-fetcher [todo-count job-channel]
  (a/go
    (loop [i 0]
      (report "Fetcher Starting")

      (when (and (< i todo-count) (not @*is-halted*))

        (do (report i "/" todo-count "jobs done.")
            (report (str "Aquiring job... [" i "/" todo-count "]"))

            (if-let [[parent job refs] (<! (get-refs job-channel))]

              (do
                (report "Refs Fetched! Putting refs...")

                (let [result
                      (<! (result-or-timeout
                           (push-refs job-channel job refs)
                           100))]

                  (if (= :success result)
                    (do (report "Job Done!")
                        (*notify* [parent job])
                        (recur (inc i)))
                    (do (report "Push Failed:" result) (halt! job-channel))
                    ))
                )

              (do (halt! job-channel))
              )
            )))

    (report "Fetcher finished!")
    ))

(defn start-fetchers [config]
  (let [thread-count (:task-count config 4)
        todo-count (:per-task config 25)

        channel-size (or (:channel-size config) 50000)
        buffer-type (if (:should-slide config) a/sliding-buffer a/buffer)
        job-channel (a/chan (buffer-type channel-size))

        threads
        (for [i (range thread-count)]
          (with-thread-name (str "F" i)
            (run-fetcher todo-count job-channel))
          )]

    [(doall threads) job-channel])
  )


(defn execute-search [initial-job config on-notification]

  (a/go
    (report "Search Starting")

    (binding [*is-halted* (atom false)
              *seen-refs* (atom #{})
              *notify* on-notification]

      (let [[threads job-channel] (start-fetchers config)]

        (report "Adding Job:" initial-job)

        (>! job-channel [nil initial-job])

        (report "Job Added")

        (doseq [thread threads] (<! thread))

        (a/close! job-channel)

        (report "Done!")

        @*is-halted*)

      )))
