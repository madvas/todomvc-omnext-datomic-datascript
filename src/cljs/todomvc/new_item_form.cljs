(ns todomvc.new-item-form
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [todomvc.util :as u]
            [clojure.string :as str]))

(defn clear-input [c]
  (om/update-state! c assoc :new-item-title ""))

(defn submit [c e]
  (let [title (str/trim (or (u/target-val e) ""))]
    (when-not (str/blank? title)
      (om/transact! c `[(todo/create-local {:db/id        -1
                                            :todo/title   ~title
                                            :todo/created ~(js/Date.)})
                        :todos/list])
      (u/prevent-default e)
      (clear-input c))))

(defn change [c e]
  (om/update-state! c assoc :new-item-title (u/target-val e)))

(defn new-item-form [c]
  (dom/input
    #js {:ref         "newField"
         :id          "new-todo"
         :value       (om/get-state c :new-item-title)
         :placeholder "What needs to be done??"
         :onChange    #(change c %)
         :onKeyDown   (u/on-key-down {:key/esc   (partial clear-input c)
                                      :key/enter (partial submit c)})}))