(ns wiki-graph.util
  (:require [malli.core :as m]
            [clojure.core.async :as a])
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
