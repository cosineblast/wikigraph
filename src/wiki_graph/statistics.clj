(ns wiki-graph.statistics
  (:require [malli.core :as m])
  )


(defn compute-most-by [key data]
  (apply max-key key data))

(defn compute-least-by [key data]
  (apply min-key key data))

(m/=> average-variance
      [:=> [:cat [:sequential number?]] [:tuple number? number?]])

(defn average-variance [stuff]
  (let [sum (apply + stuff)
        sum-squares (apply + (map * stuff stuff))
        n (count stuff)
        average (/ sum n)
        average-squares (/ sum-squares n)
        ]

    [average (- average-squares average)]
    ))



(m/=> compute-average-variance-by
      [:=> [:cat ifn? [:sequential :any]] [:tuple number? number?]])

(defn compute-average-variance-by [key data]
  (average-variance (map key data)))

(def Stats
  [:map
   [:maximum [:tuple :any number?]]
   [:minimum [:tuple :any number?]]
   [:average number?]
   [:variance number?]
   ]
  )

(m/=> get-statistics-by
      [:=>
       [:cat ifn? [:sequential [:tuple :any number?]]]
       Stats])

(defn get-statistics-by [key data]

  (let [[most least [average variance]]
        (pvalues
         (compute-most-by key data)
         (compute-least-by key data)
         (compute-average-variance-by key data)
         )]

    {:maximum [(first most) (key most)]
     :minimum [(first least) (key least)]
     :average average
     :variance variance
     }
    )
  )
