(ns todomvc.item
  (:require [clojure.string :as string]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [todomvc.util :as u :refer [hidden pluralize]]))

(defn submit [c {:keys [db/id todo/title todo/editing]} e]
  (let [edit-text (string/trim (or (om/get-state c :edit-text) ""))]
    (when (and (not (string/blank? edit-text))
               (not= edit-text title)
               editing)
      (om/transact! c `[(todo/update {:db/id ~id :todo/title ~edit-text})]))
    (u/prevent-default e)))

(defn edit [c {:keys [db/id todo/title]}]
  (om/transact! c `[(todo/edit {:db/id ~id})])
  (om/update-state! c merge {:needs-focus true :edit-text title}))

(defn cancel [c {:keys [todo/title]} e]
  (om/transact! c '[(todo/cancel-edit)])
  (om/update-state! c assoc :edit-text title)
  (u/prevent-default e))

(defn change [c e]
  (om/update-state! c assoc :edit-text (u/target-val e)))

;; -----------------------------------------------------------------------------
;; Todo Item

(defn checkbox [c {:keys [:db/id :todo/completed]}]
  (dom/input
    #js {:className "toggle"
         :type      "checkbox"
         :checked   (and completed "checked")
         :onChange  (fn [_]
                      (om/transact! c `[(todo/update {:db/id ~id :todo/completed ~(not completed)})]))}))

(defn label [c {:keys [todo/title] :as props}]
  (dom/label #js {:onDoubleClick #(edit c props)} title))

(defn delete-button [c {:keys [db/id]}]
  (dom/button
    #js {:className "destroy"
         :onClick   (fn [_]
                      (om/transact! c `[(todo/delete {:db/id ~id}) :todos/list]))}))

(defn edit-field [c props]
  (dom/input
    #js {:ref       "editField"
         :className "edit"
         :value     (om/get-state c :edit-text)
         :onBlur    #(submit c props %)
         :onChange  #(change c %)
         :onKeyDown (u/on-key-down {:key/enter (partial submit c props)
                                    :key/esc   (partial cancel c props)})}))

(defui TodoItem
  static om/IQuery
  (query [this]
    [:db/id :todo/editing :todo/completed :todo/title :todo/created])

  Object
  (componentDidUpdate [this prev-props prev-state]
    (when (and (:todo/editing (om/props this))
               (om/get-state this :needs-focus))
      (let [node (dom/node this "editField")
            len (.. node -value -length)]
        (.focus node)
        (.setSelectionRange node len len))
      (om/update-state! this assoc :needs-focus nil)))

  (render [this]
    (let [props (om/props this)
          {:keys [todo/completed todo/editing]} props
          class (cond-> ""
                        completed (str "completed ")
                        editing (str "editing"))]
      (dom/li #js {:className class}
              (dom/div #js {:className "view"}
                       (checkbox this props)
                       (label this props)
                       (delete-button this props))
              (edit-field this props)))))

(def item (om/factory TodoItem {:keyfn :db/id}))
