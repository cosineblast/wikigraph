(ns wiki-graph.fetch
  (:import [org.jsoup Jsoup])

  (:require [clojure.string :as string]
            [clojure.core.async :as a :refer [go]]
            [org.httpkit.client :as http]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]

            [malli.core :as m]

            [manifold.deferred :as d]
            )

  (:require [wiki-graph.util :refer [Chan]])
  )

(declare load-refs)

(defn- remove-unwanted-tags [doc]

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

(defn- is-ok-tag [element]

  (let [href (.attr element "href")
        title (.attr element "title")
        content (.ownText element)]

    (and
     (string/starts-with? href "/wiki/")
     (not (string/includes? href ":" )))
    )

  )

(defn- tag-to-reference [tag]
   (.substring (.attr tag "href") 6))

(defn- get-full-url [target] (str "http://en.wikipedia.org/wiki/" target))

(m/=> get-body-refs [:=> [:cat :string] [:set :string]])

(defn- get-body-refs [body]

  (let [doc (Jsoup/parse body)
        _ (remove-unwanted-tags doc)
        tags (.select doc "#bodyContent a")]


    (->> tags
         .iterator
         iterator-seq
         (filter is-ok-tag)
         (map tag-to-reference)
         (into #{}))

    ))


(declare target-exists-deferred
         fetch-wiki-refs-deferred)

(m/=> fetch-wiki-refs-async [:=> [:cat :string] Chan])

(defn fetch-wiki-refs-async [target]

  (let [channel (a/chan 1)]

    (d/on-realized
     (fetch-wiki-refs-deferred target)
     (fn [value]
       (a/put! channel {:value value})
       (a/close! channel)
       )
     (fn [error]
       (a/put! channel {:error error})
       (a/close! channel)
       )
     )

    channel
    )

  )


(defn fetch-wiki-refs-deferred [target]

  (let [deferred (d/deferred)
        url (get-full-url target)

        on-result
        (fn [{:keys [error status headers body]}]

          (cond
            error
            (d/error! deferred error)

            (not= status 200)
            (d/error! deferred status)

            :else
            (d/success! deferred (get-body-refs body) )
            )
          )]

    (http/get url nil on-result)

    deferred
    )
  )


(m/=> target-exists [:=> [:cat :string] Chan])

(defn target-exists [target]
  (let [channel (a/chan 1)]

    (d/on-realized
     (target-exists-deferred target)
     (fn [value]
       (a/put! channel value)
       (a/close! channel)
       )
     (fn [error] (go (throw error))
       (a/close! channel)
       )
     )

    channel
    ))

(defn target-exists-deferred [target]
  (let [deferred (d/deferred)
        url (get-full-url target)

        on-result
        (fn [{:keys [error status headers body]}]

          (d/success! deferred (not (or error (not= status 200))))

          )]

    (http/get url nil on-result)

    deferred
    ))
