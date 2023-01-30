(ns wiki-graph.reload)


(defn reload []

  (let [namespaces
        '{
          wiki-graph.graph :on
          wiki-graph.search :on
          wiki-graph.statistics :on
          wiki-graph.fetch :on
          wiki-graph.timing :on
          wiki-graph.report :on
          wiki-graph.core :on
          }

        on-namespaces (for [[namespace mode] namespaces :when (= mode :on)]
                        namespace)
        ]

    (doseq [namespace on-namespaces]
      (require namespace :reload))

    (println "Reloaded" (count on-namespaces) "namespaces.")

    (require 'wiki-graph.reload :reload)
    ))
