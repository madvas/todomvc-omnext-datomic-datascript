(ns todomvc.datomic
  (:require [datomic.api :as d]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [clojure.core.async :refer [go-loop]])
  (:import datomic.Util))

(defrecord DatomicDatabase [uri schema initial-data connection]
  component/Lifecycle
  (start [component]
    (d/create-database uri)
    (let [c (d/connect uri)]
      @(d/transact c schema)
      @(d/transact c initial-data)
      (assoc component :conn c
                       :tx-report-queue (d/tx-report-queue c))))
  (stop [component]
    (d/remove-tx-report-queue (:conn component))
    (d/delete-database uri)))

(defn new-database [db-uri]
  (DatomicDatabase.
    db-uri
    (first (Util/readAll (io/reader (io/resource "data/schema.edn"))))
    (first (Util/readAll (io/reader (io/resource "data/initial.edn"))))
    nil))
