(ns wiki-graph.report)


(def ^:dynamic *thread-name* nil)

(def output (agent nil))

(defn report [& args]

  (let [name (or *thread-name* "main")]

     (send output (fn [_] (apply println (cons (str "[" name "]") args))))

     nil

     ))

(defmacro with-thread-name [name & body]
  `(binding [*thread-name* ~name] ~@body)
  )
