(ns todomvc.prod-server
  (:require [todomvc.core :as todomvc])
  (:gen-class))

(def config
  {:db-uri   "datomic:mem://localhost:4334/todos"
   :web-port (or (System/getenv "PORT") 8081)})

(defn -main [& args]
  (println "Starting on port " (:web-port config))
  (todomvc/dev-start config)
  (println (str "Started server on port " (:web-port config)))
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(do (todomvc/stop)
                                  (println "Server stopped")))))