(ns wiki-graph.core
  (:gen-class)
  (:require [wiki-graph.fetch-refs :refer :all]
            [wiki-graph.report :refer :all]
            [wiki-graph.statistics :as stats])

  (:require [clojure.core.async :as a :refer [>! <! >!! <!! go]]
            [taoensso.tufte :as tufte :refer [defnp p profiled profile]]
            )
  )

(tufte/add-basic-println-handler! {})

(defn r []
  (let [namespaces '(wiki-graph.fetch-refs
                     wiki-graph.core
                     wiki-graph.statistics)]
    (doseq [namespace namespaces]
      (use namespace :reload-all))
    ))


(def result-graph (atom {}))

(def ^:dynamic *execution-limit* 65000)

(def execution-count (atom 0))

(defn run-fetcher [todo-count job-channel]
  (loop [i 0]
    (report "Fetcher Starting")

    (when (< i todo-count)

      (report (str "Aquiring Job... [" i "/" todo-count "]"))

      (let [job (<!! job-channel)
            _ (report "Job" job "aquired! Fetching Refs...")
            refs (or (@result-graph job)
                     (fetch-wikipedia-refs job))]

        (report "Refs Fetched! Putting jobs...")

        (doseq [ref refs]
          (if-not (contains? @result-graph ref)
            (>!! job-channel ref)
            ))

        (swap! result-graph assoc job refs)

        (report "Job Done!")

        (when (< (swap! execution-count inc) *execution-limit*)
          (recur (inc i)))
        ))
    )

  (report "Fetcher Done!")
  )




(defn execute-fetch-round [& args]

  (report "Program Starting")

  (let [thread-count 4
        todo-count 100
        job-channel (a/chan (+ *execution-limit* thread-count 1))
        initial-job "Communicating_sequential_processes"
        threads
        (for [i (range thread-count)]
             (a/thread

               (with-thread-name (str "F" i)
                 (run-fetcher todo-count job-channel)))
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

;;; Let's define a couple dummy fns to simulate doing some expensive work

(defn -main []
  (execute-fetch-round)
  )
