(ns wiki-graph.ring-util)

(defn handle-access-control [response]
  (assoc-in response [:headers "Access-Control-Allow-Origin"] "*"))

(defn wrap-access-control [handler]
  (fn
    ([request] (handle-access-control (handler request)))

    ([request on-response error]
     (handler request (comp on-response handle-access-control) error))
    ))

(defn ok [body]
  {:status 200
   :headers {"Content-Type" "application/json; charset=utf-8"}
   :body body})
