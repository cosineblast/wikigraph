(ns wiki-graph.fetch-refs
  (:import [org.jsoup Jsoup])
  (:require [clojure.string :as s]
            [clojure.core.async :as a]
            [org.httpkit.client :as http]

            )
  )

(declare load-refs)

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

(defn get-full-url [target] (str "http://en.wikipedia.org/wiki/" target))

(defn load-refs [doc]

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

(defn load-body [body] (load-refs (Jsoup/parse body)))

(defn fetch-wiki-refs-async [target]

  (let [config {:timeout 800 :keepalive -1}
        channel (a/chan 1)
        url (get-full-url target)

        on-result
        (fn [{:keys [error status headers body]}]

          (cond
            error (a/put! channel { :error error })
            (not= status 200) (a/put! channel { :error status } )
            :else (a/put! channel { :value (load-body body) })
            )

          (a/close! channel)
          )]

    (http/get url nil on-result)

    channel
    )

  )
