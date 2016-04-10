(defproject leinapp "1.0-SNAPSHOT"
            :main leinapp.core
            :profiles {:uberjar {:aot :all}}
            :dependencies [[org.clojure/clojure "1.7.0"]
                           [ring/ring-core "1.4.0"]
                           [ring/ring-jetty-adapter "1.4.0"]
                           [ring/ring-defaults "0.2.0"]
                           [compojure "1.5.0"]]
            :plugins [[lein-ring "0.9.7"]]
            :ring {:handler leinapp.core/app})
