(defproject om-next-demo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :jvm-opts ^:replace ["-Xms512m" "-Xmx512m" "-server"]
  :plugins [[lein-cljsbuild "1.1.3"]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.8.40"]
                 [com.datomic/datomic-free "0.9.5344"]
                 [org.omcljs/om "1.0.0-alpha32"]
                 [ring/ring "1.4.0"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [com.cognitect/transit-cljs "0.8.237"]
                 [com.stuartsierra/component "0.3.1"]
                 [com.cemerick/piggieback "0.2.1"]
                 [datascript "0.15.0"]
                 [figwheel-sidecar "0.5.0-2" :scope "test"]
                 [com.cemerick/piggieback "0.2.1"]
                 [org.clojure/tools.nrepl "0.2.10"]
                 [aleph "0.4.1-beta5"]
                 [bidi "2.0.4"]
                 [yada "1.1.5"]
                 [juxt.modular/bidi "0.9.5"]
                 [juxt.modular/maker "0.5.0"]
                 [juxt.modular/wire-up "0.5.0"]
                 [juxt.modular/aleph "0.1.4"]]

  :min-lein-version "2.0.0"
  :uberjar-name "todomvc.jar"
  :clean-targets ^{:protect false} ["resources/public/js"]
  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  ;:checkout-deps-shares ^:replace
  ;[:source-paths :resource-paths :compile-path #=(eval leiningen.core.classpath/checkout-deps-paths)]

  :cljsbuild {:builds {:app {:source-paths ["src/cljs" "src/cljc"]
                             :figwheel     true
                             :compiler     {:main               todomvc.core
                                            :output-to          "resources/public/js/app.js"
                                            :output-dir         "resources/public/js"
                                            :asset-path         "/js"
                                            :optimizations      :simple
                                            :static-fns         true
                                            :optimize-constants true
                                            :pretty-print       true
                                            :externs            ["src/js/externs.js"]
                                            :closure-defines    {goog.DEBUG false}
                                            :parallel-build     true
                                            :verbose            true}}}}

  :profiles {:dev     {:source-paths ["env/dev/clj"]
                       :main         todomvc.dev-server
                       :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                       :plugins      [[lein-figwheel "0.5.1"]]
                       :figwheel     {:server-port 3449}}
             :uberjar {:source-paths ["env/prod/clj"]
                       :main         todomvc.prod-server
                       :hooks        [leiningen.cljsbuild]
                       :aot          :all
                       :omit-source  true
                       :cljsbuild    {:builds {:app
                                               {:compiler {:optimizations   :advanced
                                                           :closure-defines {:goog.DEBUG false}
                                                           :pretty-print    false}}}}
                       }}

  )
