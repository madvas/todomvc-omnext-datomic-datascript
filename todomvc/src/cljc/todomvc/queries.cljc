(ns todomvc.queries
  (:require
    #?(:clj [datomic.api :as d]
       :cljs [datascript.core :as d])
            ))


(defn todos
  ([db]
   (todos db nil))
  ([db selector]
   (todos db selector nil))
  ([db selector {:keys [cond as-of]}]
   (let [;db (cond-> db as-of (d/as-of as-of))
         q (cond->
             '[:find [(pull ?eid selector) ...]
               :in $ selector
               :where
               [?eid :todo/created]]
             (map? cond) (concat (map (partial cons '?eid) (vec cond)))
             )]
     (d/q q (d/db db) (or selector '[*])))))

(defn pull
  ([db id]
   (pull db id '[*]))
  ([db id query]
   (d/pull (d/db db) (or query '[*]) id)))

(defn retract-entity [conn id]
  (d/transact conn [[:db.fn/retractEntity id]]))

(defn todo-update [conn {:keys [db/id todo/completed todo/title]}]
  (d/transact conn [(merge {:db/id id}
                           (when (or (true? completed) (false? completed))
                             {:todo/completed completed})
                           (when title
                             {:todo/title title}))]))

(defn add-entity [conn item]
  (d/transact conn [item]))

(defn todos-delete-by [conn cond-map]
  (let [todos (todos conn [:db/id] {:cond cond-map})
        tx-data (map #(vec [:db.fn/retractEntity (:db/id %)]) todos)]
    (d/transact conn tx-data)))


(defn todos-toggle-all [conn value]
  (let [all-todos (todos conn [:db/id])]
    (d/transact conn (map (partial merge {:todo/completed value}) all-todos))))

(defn update-ids! [conn ids id-key]
  (->> (d/pull-many (d/db conn) '[*] (keys ids))
       (map #(assoc % id-key (get ids (% id-key))))
       (concat (map #(vec [:db.fn/retractEntity %]) (keys ids)))
       (d/transact conn)))