(ns wiki-graph.statistics)




(defn compute-most-by [key data]
  (apply max-key key data))

(defn compute-least-by [key data]
  (apply min-key key data))

(defn average-variance [stuff]
  (let [sum (apply + stuff)
        sum-squares (apply + (map * stuff stuff))
        n (count stuff)
        average (/ sum n)
        average-squares (/ sum-squares n)
        ]

    [average (- average-squares average)]
    ))

(defn compute-average-variance-by [key data]
  (average-variance (map key data)))

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
