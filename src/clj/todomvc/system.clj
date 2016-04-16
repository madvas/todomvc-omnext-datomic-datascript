(ns todomvc.system
  (:require [com.stuartsierra.component :as c]
            todomvc.datomic
            todomvc.website
            todomvc.datomic
            todomvc.server-sent-events
            [modular.maker :as mm]
            [modular.bidi :refer [new-router new-web-resources]]
            [modular.aleph :refer [new-webserver]]
            [todomvc.util :as u]))

(defn http-listener-components [system config]
  (assoc system :http-listener (new-webserver :port (u/s->int (:web-port config)))))


(defn modular-bidi-router-components [system config]
  (assoc system :bidi-request-handler (mm/make new-router config)))

(defn website-components [system config]
  (assoc system :todomvc-website
                (-> (mm/make todomvc.website/new-website config)
                    (c/using []))))

(defn datomic-component [system config]
  (assoc system :datomic (todomvc.datomic/new-database (:db-uri config))))

(defn sse-component [system config]
  (assoc system :server-sent-events (todomvc.server-sent-events/new-server-sent-events)))

(defn new-dependency-map []
  {:http-listener        {:request-handler :bidi-request-handler}
   :bidi-request-handler {:sse     :server-sent-events
                          :website :todomvc-website}
   :todomvc-website      {:db  :datomic
                          :sse :server-sent-events}})

(defn new-system-map
  [config]
  (apply c/system-map
         (apply concat
                (-> {}
                    (http-listener-components config)
                    (modular-bidi-router-components config)
                    (website-components config)
                    (datomic-component config)
                    (sse-component config)))))

(defn dev-system
  ([] (dev-system {}))
  ([config]
   (-> (new-system-map config)
       (c/system-using (new-dependency-map)))))
