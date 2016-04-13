(ns todomvc.figwheel
  (:require [figwheel-sidecar.repl :as r]
            [figwheel-sidecar.repl-api :as ra]
            [cljs.build.api :as b]))

(comment
  (b/build "src/cljs"
           {:output-to          "resources/public/js/app.js"
            :output-dir         "resources/public/js"
            :optimizations      :simple
            :static-fns         true
            :optimize-constants true
            :pretty-print       true
            :externs            ["src/js/externs.js"]
            :closure-defines    '{goog.DEBUG false}
            :verbose            true})

  )

(defn start-fig! []
  (ra/start-figwheel!
    {:figwheel-options {}
     :build-ids        ["dev"]
     :all-builds       [{:id           "dev"
                         :figwheel     true
                         :source-paths ["src/cljs" "src/cljc"]
                         :compiler     {:main           'todomvc.core
                                        :asset-path     "/js"
                                        :output-to      "resources/public/js/app.js"
                                        :output-dir     "resources/public/js"
                                        :parallel-build true
                                        :compiler-stats true
                                        :verbose        true}}]}))

(ra/cljs-repl)
