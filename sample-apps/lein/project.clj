(defproject leinapp "1.0-SNAPSHOT"
            :main leinapp.core
            :profiles {:uberjar {:aot :all}}
            :dependencies [[org.clojure/clojure "1.7.0"]
                           [ring/ring-core "1.5.0"]
                           [ring/ring-jetty-adapter "1.5.0"]
                           [ring/ring-defaults "0.2.1"]
                           [compojure "1.5.1"]]
            :plugins [[lein-ring "0.10.0"]]
            :ring {:handler leinapp.core/app})
