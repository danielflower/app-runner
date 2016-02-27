(defproject leinapp "0.1.0-SNAPSHOT"
  :description "App name"
  :main leinapp.core
  :profiles {:uberjar {:aot :all}}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]])
