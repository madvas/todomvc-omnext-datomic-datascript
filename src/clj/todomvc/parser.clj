(ns todomvc.parser
  (:refer-clojure :exclude [read])
  (:require [todomvc.queries :as q]
            [todomvc.util :as u]
            [datomic.api :as da]
            [clojure.walk :as walk]))

;; =============================================================================
;; Reads

(defmulti readf (fn [env k params] k))

(defmethod readf :default
  [_ k _]
  (println "default read " k)
  {:value {:error (str "No handler for read key " k)}})

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
  {:action (fn []
             (-> @(q/add-entity! conn {:db/id          #db/id [:db.part/user]
                                       :todo/title     title
                                       :todo/completed false
                                       :todo/created   (java.util.Date.)})
                 (update :tempids #(walk/prewalk (fn [x]
                                                   (if (map-entry? x)
                                                     [id (val x)]
                                                     x)) %))))})


(defmethod mutatef 'todo/update
  [{:keys [conn]} _ todo]
  {:action (fn []
             @(q/todo-update! conn todo))})

(defmethod mutatef 'todo/delete
  [{:keys [conn]} _ {:keys [db/id]}]
  {:action (fn []
             @(q/retract-entity! conn id))})

(defmethod mutatef 'todos/delete-by
  [{:keys [conn]} _ cond-map]
  {:action (fn []
             @(q/todos-delete-by! conn cond-map))})

(defmethod mutatef 'todos/toggle-all
  [{:keys [conn]} _ {:keys [value]}]
  {:action (fn []
             @(q/todos-toggle-all! conn value))})
