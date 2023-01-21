(ns wiki-graph.core
  (:gen-class)
  (:require [wiki-graph.fetch-refs :refer :all]
            [wiki-graph.report :refer :all]
            [wiki-graph.statistics :as stats]
            [wiki-graph.reload :refer [r]]
            [wiki-graph.timing :refer [timing time!]]
            )

  (:require [clojure.core.async :as a :refer [>! <! >!! <!! go]])

  )


(def result-graph (atom {}))

(def ^:dynamic *execution-limit* 65000)

(def execution-count (atom 0))

(def is-halted (atom false))

(def seen-refs (atom #{}))

(defn get-refs [job-channel]
  (a/go

    (when-let [job (<! job-channel)]

      (report "Job" job "aquired! Getting Refs...")

      (if-let [cached (@result-graph job)]

        (do (report "Job was cached!") [job cached])

        (let [{:keys [error value]} (<! (fetch-wiki-refs-async job))]
          (if error
            (do (report "Error while fetching refs:" error) nil)

            [job value]
            )

          ))

      ))
  )

(defn push-refs [job-channel job refs]
  (a/go

    (doseq [ref refs]
      (if-not (contains? @seen-refs ref)
        (>! job-channel ref)
        ))

    (swap! result-graph assoc job refs)
    (swap! seen-refs conj job)
    ))

(defn halt! [job-channel]
  (report "HALTING")
  (reset! is-halted true)
  (a/close! job-channel)
  )

(defn run-fetcher [todo-count job-channel]
  (a/go
    (loop [i 0]
      (report "Fetcher Starting")

      (when (and (< i todo-count) (not @is-halted))

        (do
          (report i "/" todo-count "jobs done.")
          (report (str "Aquiring job... [" i "/" todo-count "]"))

          (if-let [[job refs] (<! (get-refs job-channel))]

            (do
              (report "Refs Fetched! Putting refs...")

              (<! (push-refs job-channel job refs))

              (report "Job Done!")

              (when (< (swap! execution-count inc) *execution-limit*)
                (recur (inc i))))

            (halt! job-channel)
            )
          )))

    (report "Fetcher finished!")
    ))


(defn execute-fetch-round [& args]

  (report "Program Starting")

  (let [thread-count 4
        todo-count 10
        job-channel (a/chan (+ *execution-limit* thread-count 1))
        initial-job "Communicating_sequential_processes"
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



(defn dump-round-output [graph job-channel]

  (let [density-stats (stats/get-density-statistics @result-graph)
        word-stats (stats/get-word-statistics @result-graph)
        remaining-work (channel-to-vector job-channel)
        filename "current.json"]

    (println "Density Stats:")
    (println density-stats)

    (println "Word Stats:")
    (println word-stats)

    (println "Writing state to" filename)
    )
  )

(defn -main []

  (execute-fetch-round)
  )
