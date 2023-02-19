(ns wiki-graph.util
  (:require [malli.core :as m]
            [clojure.core.async :as a]
            [manifold.stream :as s])

  (:import [clojure.core.async.impl.channels ManyToManyChannel])
  )

(m/=> chan? [:=> [:cat :any] :boolean])

(defn chan? [x]
  (instance? ManyToManyChannel x))

(def Chan [:fn
           {:title "Chan"
            :description "A clojure async ManyToManyChannel."}
           chan?])

(def Deferred :any)

(def Stream :any)

(defn offer-onto-chan
  "Puts the contents of coll into the supplied channel with offer!.

  If the close? argument is true (the default),
  the function closes the channel before returning.

  Returns true if and only if all elements of coll
  where successfully offered onto the channel.
  "

  ([channel coll close?]

   (let [result
         (loop [values coll]

           (if-let [[value & more] (seq values)]

             (if (a/offer! channel value)
               (recur more)
               false)

             true
             ))]

     (when close? (a/close! channel))

     result
     )
   )

  ([channel coll] (offer-onto-chan channel coll true))
  )

(defn offer-onto-stream
  "Puts the contents of coll into the supplied stream
  without allowing any timeout on puts.

  If the close? argument is true (the default),
  the function closes the channel before returning.

  Returns true if and only if all elements of coll
  where successfully offered onto the stream.
  "

  ([stream coll close?]

   (let [result
         (loop [values coll]

           (if-let [[value & more] (seq values)]

             ;; TODO: make this function return a deferred.
             (if @(s/try-put! stream value 0)
               (recur more)
               false)

             true
             ))]

     (when close? (s/close! stream))

     result
     )
   )

  ([channel coll] (offer-onto-chan channel coll true))
  )
