(ns wiki-graph.reload)


(defn r []

  (let [namespaces
        '{wiki-graph.fetch-refs :on
          wiki-graph.core :on
          wiki-graph.statistics :off
          wiki-graph.timing :on
          }

        on-namespaces (for [[namespace mode] namespaces :when (= mode :on)]
                        namespace)
        ]

    (doseq [namespace on-namespaces]
      (require namespace :reload))

    (println "Reloaded" (count on-namespaces) "namespaces.")

    (require 'wiki-graph.reload :reload)
    ))
