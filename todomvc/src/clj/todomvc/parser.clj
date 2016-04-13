(ns todomvc.parser
  (:refer-clojure :exclude [read])
  (:require [todomvc.queries :as q]
            [om.tempid :as omtmp]
            [todomvc.util :as u]))

;; =============================================================================
;; Reads

(defmulti readf (fn [env k params] k))

(defmethod readf :default
  [_ k _]
  {:value {:error (str "No handler for read key " k)}})

(defmethod readf :todos/by-id
  [{:keys [conn query query-root]} _ _]
  {:value (q/pull conn (second query-root) query)})

(defmethod readf :todos/list
  [{:keys [conn query]} _ params]
  {:value (q/todos conn query params)})

;; =============================================================================
;; Mutations

(defmulti mutatef (fn [env k params] k))

(defmethod mutatef :default
  [_ k _]
  {:value {:error (str "No handler for mutation key " k)}})

(defmethod mutatef 'todo/create
  [{:keys [conn]} _ {:keys [todo/title db/id]}]
  {;:value  {:keys [:todos/list]}
   :action (fn []
             (try
               (-> @(q/add-entity conn {:db/id          #db/id [:db.part/user]
                                        :todo/title     title
                                        :todo/completed false
                                        :todo/created   (java.util.Date.)})
                   (update :tempids #(assoc (first %) 0 id)))
               (catch Exception e
                 (println (str "caught exception: " (.getMessage e))))))})


(defmethod mutatef 'todo/update
  [{:keys [conn]} _ todo]
  {:value  {:keys [[:todos/by-id (:db/id todo)]]}
   :action (fn []
             (try
               @(q/todo-update conn todo)
               (catch Exception e
                 (println (str "caught exception: " (.getMessage e))))))})

(defmethod mutatef 'todo/delete
  [{:keys [conn]} _ {:keys [db/id]}]
  {:value  {:keys [:todos/list]}
   :action (fn []
             @(q/retract-entity conn id))})

(defmethod mutatef 'todos/delete-by
  [{:keys [conn]} _ cond-map]
  {:value  {:keys [:todos/list]}
   :action (fn []
             (try
               @(q/todos-delete-by conn cond-map)
               (catch Exception e
                 (println (str "caught exception: " (.getMessage e))))))})

(defmethod mutatef 'todos/toggle-all
  [{:keys [conn]} _ {:keys [value]}]
  {:value  {:keys [:todos/list]}
   :action (fn []
             (try
               @(q/todos-toggle-all conn value)
               (catch Exception e
                 (println (str "caught exception: " (.getMessage e))))))})

(comment
  (require '[todomvc.core :as cc]
           '[om.next.server :refer [parser]]
           '[clojure.pprint :as pprint])

  (cc/dev-start))
