(ns wiki-graph.fetch
  (:import [org.jsoup Jsoup])

  (:require [clojure.string :as string]
            [clojure.core.async :as a]
            [org.httpkit.client :as http]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as st]

            [malli.core :as m]
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


(m/=> fetch-wiki-refs-async [:=> [:cat :string] Chan])

(defn fetch-wiki-refs-async [target]

  (let [config {:timeout 800 :keepalive -1}
        channel (a/chan 1)
        url (get-full-url target)

        on-result
        (fn [{:keys [error status headers body]}]

          (cond
            error (a/put! channel { :error error })
            (not= status 200) (a/put! channel { :error status } )
            :else (a/put! channel { :value (get-body-refs body) })
            )

          (a/close! channel)
          )]

    (http/get url nil on-result)

    channel
    )
  )

(m/=> target-exists [:=> [:cat :string] Chan])

(defn target-exists [target]
  (let [config {:timeout 800 :keepalive -1}
       channel (a/chan 1)
       url (get-full-url target)

       on-result
       (fn [{:keys [error status headers body]}]

         (if (or error (not= status 200))
           (a/put! channel false)
           (a/put! channel true))

         (a/close! channel)
         )]

  (http/get url nil on-result)

  channel
  ))
