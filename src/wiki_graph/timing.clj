(ns wiki-graph.timing)

(def ^:dynamic *reference-time* nil)

(defmacro timing [& body]
  `(binding [*reference-time* (System/currentTimeMillis)] ~@body)
  )

(defn time! []
  (let [current-time (System/currentTimeMillis)]
    (if (some? *reference-time*)
      (-  current-time *reference-time*)
      current-time
      ))

  )
