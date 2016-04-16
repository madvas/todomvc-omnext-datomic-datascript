(ns todomvc.core
  (:require [com.stuartsierra.component :as component]
            [todomvc.system :as system]))

(def servlet-system (atom nil))

;; =============================================================================
;; Development

(def dev-config
  {:db-uri   "datomic:mem://localhost:4334/todos"
   :web-port 8081})

(def prod-config
  {:db-uri   "datomic:mem://localhost:4334/todos"
   :web-port (or (System/getenv "PORT") 8081)})

(defn dev-start []
  (let [sys  (system/dev-system dev-config)
        sys' (component/start sys)]
    (reset! servlet-system sys')
    (println "System started")
    sys'))

(defn stop []
  (swap! servlet-system component/stop)
  (println "System stopped"))

(defn dev-restart []
  (stop)
  (dev-start))



