(ns todomvc.parser
  (:require [om.next :as om]
            [datascript.core :as d]
            [todomvc.queries :as q]
            [todomvc.util :as u]))

;; =============================================================================
;; Reads

(defmulti read om/dispatch)

(defmethod read :default
  [{:keys [state]} k _]
  (println "Default read " k)
  {:remote true})

(defmethod read :todos/list
  [{:keys [state query] :as p}]
  ; To know if this method is called by Todos
  ; component or as second argument in mutation query expression in om.next/transact!
  ; not very elegant
  {:remote (not (or (contains? p :reconciler) (contains? p :component)))
   :value  (q/todos state query)})

;; =============================================================================
;; Mutations

(defmulti mutate om/dispatch)

(defmethod mutate :default
  [_ _ _]
  {:remote true})

(defmethod mutate 'todos/toggle-all
  [{:keys [state]} _ {:keys [value]}]
  {:value  {:keys [:todos/list]}
   :remote true
   :action (fn []
             (q/todos-toggle-all! state value))})

(defn clear-editing! [state]
  (d/transact! state (into [] (map #(assoc % :todo/editing false) (q/todos state [:db/id])))))

(defmethod mutate 'todo/update
  [{:keys [state]} _ todo]
  {:remote true
   :action (fn []
             (clear-editing! state)
             (q/todo-update! state todo))})

(defmethod mutate 'todo/edit
  [{:keys [state]} _ {:keys [db/id]}]
  {:remote false
   :value  {:keys [:todos/list]}
   :action (fn []
             (clear-editing! state)
             (d/transact! state [{:db/id        id
                                  :todo/editing true}]))})

(defmethod mutate 'todo/cancel-edit
  [{:keys [state]} _ _]
  {:action (fn []
             (clear-editing! state))})

(defmethod mutate 'todo/create-local
  [{:keys [state component]} _ {:keys [db/id] :as new-todo}]
  {:value  {:keys [:todos/list]}
   :remote false
   :action (fn []
             (let [local-id (-> @(q/add-entity! state new-todo)
                                :tempids
                                (get id))]
               (om/transact! component `[(todo/create ~(assoc new-todo :db/id local-id))])))})

(defmethod mutate 'todo/delete
  [{:keys [state]} _ {:keys [db/id]}]
  {:value  {:keys [:todos/list]}
   :remote true
   :action (fn []

             (q/retract-entity! state id))})

(defmethod mutate 'todos/delete-by
  [{:keys [state]} _ cond-map]
  {:value  {:keys [:todos/list]}
   :remote true
   :action (fn []
             (q/todos-delete-by! state cond-map))})

(defmethod mutate 'todos/write-tx-data
  [{:keys [state]} _ {:keys [tx-data]}]
  {:remote false
   :action (fn []
             (q/write-tx-data! state tx-data))})

(defmethod mutate 'todos/print
  [{:keys [state]}]
  {:remote false
   :action (fn []
             (u/pcoll (q/todos state)))})


