(ns wiki-graph.statistics)

(def children-count (comp count second))


(defn compute-most-dense-entry [data]
  (apply max-key children-count data))

(defn compute-least-dense-entry [data]
  (apply min-key children-count data))

(defn average-variance [stuff]
  (let [sum (apply + stuff)
        sum-squares (apply + (map * stuff stuff))
        n (count stuff)
        average (/ sum n)
        average-squares (/ sum-squares n)
        ]

    [average (- average-squares average)]


    ))

(defn compute-average-variance-entry-density [data]
  (average-variance (map children-count data)))



(defn get-density-statistics [data]

  (let [
        [most least [average variance]]
        (pvalues
         (compute-most-dense-entry data)
         (compute-least-dense-entry data)
         (compute-average-variance-entry-density data)
         )
        ]
    {:maximum [(first most) (count (first most))]
     :minimum [(first least) (count (first least))]
     :average average
     :variance variance
     }
    )
  )

(defn get-word-statistics [data]
  )
