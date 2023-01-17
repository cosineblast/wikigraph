(ns wiki-graph.fetch-refs
  (:import [org.jsoup Jsoup])
  (:require [clojure.string :as s]
            [clojure.core.async :as a]
            )
  )


(defn remove-unwanted-tags [doc]

  (let [kill (fn [target]
               (->
                target
                (.get 0)
                (.parent)
                (.nextElementSiblings)
                (.remove))
               )

        references (.select doc "#References")]

    (if (.isEmpty references)

      (let [external-links (.select doc "#External_links")]
        (println "No references:" (.title doc))

        (when-not (.isEmpty external-links)
          (kill external-links))
        )

      (kill references)
      )

    )

  )

(defn is-ok-tag [element]

  (let [href (.attr element "href")
        title (.attr element "title")
        content (.ownText element)]

    (and
     (s/starts-with? href "/wiki/")
     (not (s/includes? href ":" )))
    )

  )

(defn tag-to-reference [tag]
   (.substring (.attr tag "href") 6))


(defn fetch-wikipedia-refs [target]
  (let [url (str "https://en.wikipedia.org/wiki/" target)
        doc (.get (Jsoup/connect url))
        ]

    (remove-unwanted-tags doc)

    (let [tags (.select doc "#bodyContent a")]

      (->> tags
           .iterator
           iterator-seq
           (filter is-ok-tag)
           (map tag-to-reference)
           (into #{}))
      )

    )
  )

(defn fetch-wikipedia-refs-channel [target]

  (a/thread (fetch-wikipedia-refs target))

  )
