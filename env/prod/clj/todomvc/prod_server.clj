(ns todomvc.prod-server
  (:require [todomvc.core :as todomvc]))

(defn -main [& args]
  (todomvc/dev-start)
  (println (str "Started server on port " (:web-port todomvc/prod-config)))
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(do (todomvc/stop)
                                  (println "Server stopped")))))