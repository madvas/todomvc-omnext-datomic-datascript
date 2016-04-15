(ns todomvc.dev-server
  (:require [todomvc.core :as todomvc]
            [todomvc.figwheel :as tf]))


; (defn -main [& args]
(todomvc/dev-start)
(println (str "Started server on port " (:web-port todomvc/dev-config)))
(.addShutdownHook (Runtime/getRuntime)
                  (Thread. #(do (todomvc/stop)
                                (println "Server stopped"))))
(tf/start-fig!)
;)