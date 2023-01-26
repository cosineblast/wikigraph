(ns wiki-graph.search
  (:require [clojure.core.async :as a :refer [>! >!! <! <!!]])

  (:require [wiki-graph.graph :as graph]
            [wiki-graph.fetch-refs :refer [fetch-wiki-refs-async]]
            [wiki-graph.report :refer [report with-thread-name]]
            ))


(def is-halted (atom false))

(def seen-refs (atom #{}))

(defn get-refs [job-channel]
  (a/go

    (when-let [job (<! job-channel)]

      (report "Job" job "aquired! Getting Refs...")

      (if-let [cached (graph/get job)]

        (do (report "Job was cached!") [job cached])

        (let [{:keys [error value]} (<! (fetch-wiki-refs-async job))]
          (if error
            (do (report "Error while fetching refs:" error) nil)

            [job value]
            )

          ))

      ))
  )

(defn push-refs [job-channel job input-refs]
  (a/go

    (graph/assoc job input-refs)
    (swap! seen-refs conj job)

    (loop [refs (filter (comp not @seen-refs) input-refs)]

      (if-let [[ref & more] (seq refs)]
        (if (>! job-channel ref)
          (recur more)
          :closed)
        :success
        ))

    ))

(defn halt! [job-channel]
  (report "HALTING")
  (reset! is-halted true)
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

      (when (and (< i todo-count) (not @is-halted))

        (do (report i "/" todo-count "jobs done.")
            (report (str "Aquiring job... [" i "/" todo-count "]"))

            (if-let [[job refs] (<! (get-refs job-channel))]

              (do (report "Refs Fetched! Putting refs...")

                  (let [result
                        (<! (result-or-timeout
                             (push-refs job-channel job refs)
                             100))]

                    (if (= :success result)
                      (do (report "Job Done!") (recur (inc i)))
                      (do (report "Push Failed:" result) (halt! job-channel))
                      ))
                  )

              (do (halt! job-channel))
              )
            )))

    (report "Fetcher finished!")
    ))


(defn execute-search [& args]

  (report "Program Starting")

  (reset! is-halted false)

  (reset! seen-refs #{})

  (let [thread-count 4
        todo-count 1000
        job-channel (a/chan 650000)
        initial-job "Clojure"
        threads
        (for [i (range thread-count)]
               (with-thread-name (str "F" i)
                 (run-fetcher todo-count job-channel))
             )]

    (dorun threads)

    (report "Adding Job:" initial-job)

    (>!! job-channel initial-job)

    (report "Job Added")

    (doseq [thread threads] (<!! thread))

    (a/close! job-channel)

    (report "Done!")
    ))

(defn channel-to-vector [channel]
  (loop [result []]
    (if-let [next (<!! channel)]
      (recur (conj result next))
      result
      )
    ))
