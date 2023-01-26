(ns wiki-graph.reload)


(defn reload []

  (let [namespaces
        '{wiki-graph.fetch-refs :off
          wiki-graph.core :on
          wiki-graph.statistics :off
          wiki-graph.timing :off
          wiki-graph.graph :off
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
