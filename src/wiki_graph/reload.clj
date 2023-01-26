(ns wiki-graph.reload)


(defn r []

  (let [namespaces
        '{wiki-graph.fetch-refs :off
          wiki-graph.core :off
          wiki-graph.statistics :on
          wiki-graph.timing :off
          wiki-graph.graph :on
          wiki-graph.search :on
          }

        on-namespaces (for [[namespace mode] namespaces :when (= mode :on)]
                        namespace)
        ]

    (doseq [namespace on-namespaces]
      (require namespace :reload))

    (println "Reloaded" (count on-namespaces) "namespaces.")

    (require 'wiki-graph.reload :reload)
    ))
