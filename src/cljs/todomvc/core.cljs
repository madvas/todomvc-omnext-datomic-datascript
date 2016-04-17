(ns todomvc.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [todomvc.util :as util :refer [hidden pluralize]]
            [todomvc.item :as item]
            [todomvc.new-item-form :as new-item-form]
            [todomvc.parser :as p]
            [todomvc.util :as u]
            [clojure.string :as str]
            [datascript.core :as d]
            [todomvc.queries :as q]
            [goog.events :as e]
            [goog.events.EventType :as et])
  (:import [goog History]
           [goog.history EventType]))

(enable-console-print!)


;; -----------------------------------------------------------------------------
;; Components

; Weird bug?, if DB is empty and app loads no todos from server Om.Next replaces datascript DB
; with atom
(def conn (d/conn-from-datoms [(d/datom -1 :dummy/item 1)]))

#_(d/listen! conn :log (fn [tx-report]
                         (->> (:tx-data tx-report)
                              (filter #(true? (last %)))
                              (map #(apply println (take 3 %)))
                              doall)))

(defn show-item? [filter-type item]
  (let [completed? (:todo/completed item)]
    (or (and completed? (= filter-type :completed))
        (and (not completed?) (= filter-type :active))
        (str/blank? filter-type))))

(defn main [todos {:keys [todos/list]}]
  (let [checked? (every? :todo/completed list)
        {sort-by     :sort-by
         sort-dir    :sort-dir
         filter-type :filter-type} (om/get-state todos)]
    (dom/section #js {:id "main" :style (hidden (empty? list))}
                 (dom/input
                   #js {:id       "toggle-all"
                        :type     "checkbox"
                        :onChange #(om/transact! todos `[(todos/toggle-all {:value ~(not checked?)})
                                                         :todos/list])
                        :checked  checked?})
                 (apply dom/ul #js {:id "todo-list"}
                        (->> list
                             (filter (partial show-item? filter-type))
                             (u/sort-by-dir sort-by sort-dir)
                             (map item/item))))))

(defn clear-button [todos completed]
  (when (pos? completed)
    (dom/button
      #js {:id      "clear-completed"
           :onClick (fn [_] (om/transact! todos `[(todos/delete-by {:todo/completed true}) :todos/list]))}
      (str "Clear completed (" completed ")"))))

(defn footer [todos props active completed]
  (let [filter-type (:filter-type (om/get-state todos))]
    (dom/footer #js {:id "footer"}
                (dom/span #js {:id "todo-count"}
                          (dom/strong nil active)
                          (str " " (pluralize active "item") " left"))
                (apply dom/ul #js {:id "filters"}
                       (map (fn [[x y]]
                              (dom/li nil
                                      (dom/a #js {:href      (str "#/" (name x))
                                                  :className (when (or (= x filter-type)) "selected")
                                                  :onClick   #(om/update-state! todos assoc :filter-type x)}
                                             y)))
                            [["" "All"] [:active "Active"] [:completed "Completed"]]))
                (clear-button todos completed))))

(defn parse-delta [delta]
  (->> (for [[action value] (remove #(symbol? (key %)) delta)]
         (if (keyword? action)
           {:keys  [action]
            :value value}
           {:keys  action                                   ; I'm not sure about correctness of this at all
            :value [value]}))                               ; Throws some error into console, but it works ;)
       (reduce (partial merge-with concat))))

(defn merge-delta [conn]
  (fn [reconciler state res]
    (let [{:keys [keys value]} (parse-delta res)]
      (d/transact conn value)
      {:keys    keys
       :next    @conn
       :tempids (->> (filter (comp symbol? first) res)
                     (map (comp :tempids :result second))
                     (reduce merge {}))})))

(defn migrate-tempids [app-state-pure _ tempids id-key]
  (when-not (empty? tempids)                                ; Migrate should return updated db
    (q/update-ids! conn tempids id-key))                    ; instead of making direct changes to it
  app-state-pure)                                           ; but it didn't work here with datascript

(def reconciler
  (om/reconciler
    {:state     conn
     :normalize false
     :parser    (om/parser {:read p/read :mutate p/mutate})
     :send      (util/transit-post "/api")
     :merge     (merge-delta conn)
     :id-key    :db/id
     :migrate   migrate-tempids}))

(defui Todos
  static om/IQueryParams
  (params [this]
    {:todo-item (om/get-query item/TodoItem)})

  static om/IQuery
  (query [this]
    '[({:todos/list ?todo-item})])

  Object
  (componentWillMount [this]
    (let [loc (.substring js/window.location.hash 2)
          es (new js/EventSource "/events")]
      (e/listen es et/MESSAGE
                (fn [e]
                  (let [msg (u/read-transit (u/event-data e))]
                    (migrate-tempids nil nil (:tempids msg) (:id-key (:config reconciler)))
                    (om/transact! this `[(todos/write-tx-data ~msg) :todos/list]))))
      (om/set-state! this {:event-source es
                           :sort-by      :todo/created
                           :sort-dir     :desc
                           :filter-type  (u/apply-if str/blank? keyword loc)})))

  (componentWillUnmount [this]
    (when-let [es (:event-source (om/get-state this))]
      (e/removeAll es et/MESSAGE)))
  (render [this]
    (let [props (om/props this)
          {:keys [todos/list]} props
          active (count (remove :todo/completed list))
          completed (- (count list) active)]
      (dom/div nil
               (dom/header #js {:id "header"}
                           (dom/h1 #js {:onClick #(om/transact! this `[(todos/print)])} "todos")
                           (new-item-form/new-item-form this)
                           (main this props)
                           (footer this props active completed))))))

(om/add-root! reconciler Todos (gdom/getElement "todoapp"))

