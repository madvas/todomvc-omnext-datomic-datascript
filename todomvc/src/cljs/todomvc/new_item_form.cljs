(ns todomvc.new-item-form
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [todomvc.util :as u]
            [clojure.string :as string]))


(defn clear-input [c]
  (om/update-state! c assoc :new-item-title ""))


(defn submit [c props e]
  (let [title (string/trim (or (u/target-val e) ""))]
    (when-not (string/blank? title)
      (om/transact! c `[(todo/create-local {:db/id        -1
                                            :todo/title   ~title
                                            :todo/created ~(js/Date.)})
                        :todos/list])
      (u/prevent-default e)
      (clear-input c))))



(defn change [c e]
  (om/update-state! c assoc :new-item-title (u/target-val e)))

(defui TodoNewItemForm
  static om/IQuery
  (query [this]
    '[])

  Object
  (render [this]
    (let [props [(om/props this)]]
      (dom/input
        #js {:ref         "newField"
             :id          "new-todo"
             :value       (om/get-state this :new-item-title)
             :placeholder "What needs to be done??"
             :onChange    #(change this %)
             :onKeyDown   (u/on-key-down this props {:escape-key clear-input
                                                     :enter-key  submit})}))))

(def new-item-form (om/factory TodoNewItemForm))