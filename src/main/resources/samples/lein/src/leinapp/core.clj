(ns leinapp.core
  (:gen-class))
(use 'ring.adapter.jetty)

(def app-port (Integer/parseInt (get (System/getenv) "APP_PORT" "8082")))
(def app-name (get (System/getenv) "APP_NAME" "leinapp"))

(defn app-handler [request]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (str "Hello from " app-name)})

(defn -main []
  (run-jetty app-handler {:port app-port}))
