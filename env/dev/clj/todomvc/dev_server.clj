(ns todomvc.dev-server
  (:require [todomvc.core :as todomvc]
            [todomvc.figwheel :as tf]))


(def config
  {:db-uri   "datomic:mem://localhost:4334/todos"
   :web-port 8081})

(defn -main [& args]
  (todomvc/dev-start config)
  (println (str "Started server on port " (:web-port config)))
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. #(do (todomvc/stop)
                                  (println "Server stopped"))))
  (tf/start-fig!))