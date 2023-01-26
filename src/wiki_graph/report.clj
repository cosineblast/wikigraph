(ns wiki-graph.report)


(def ^:dynamic *thread-name* nil)


(defn report [& args]

  (let [name (or *thread-name* "main")]

     (locking report
       ;;(apply println (cons (str "[" name "]") args))
       )
     ))

(defmacro with-thread-name [name & body]
  `(binding [*thread-name* ~name] ~@body)
  )
