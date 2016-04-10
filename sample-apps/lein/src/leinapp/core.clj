(ns leinapp.core
    (:gen-class)
    (:require [compojure.route :as route]
      [ring.util.response :as response]
      [ring.middleware.defaults :as defaults])
    (:use compojure.core
      ring.adapter.jetty))

(def app-port (Integer/parseInt (get (System/getenv) "APP_PORT" "8082")))
(def app-name (get (System/getenv) "APP_NAME" "leinapp"))
(def context-path (str "/" app-name))
(defroutes
  main-routes
  (GET "/" [] (response/redirect context-path))
  (context context-path []
           (GET "/" [] "Hello from lein")
           (route/resources "/")
           (route/not-found "Page not found")))

(def app
  (defaults/wrap-defaults main-routes defaults/site-defaults))

(defn -main []
      (println "Server started at " (str "http://localhost:" app-port context-path))
      (run-jetty app {:port app-port}))
