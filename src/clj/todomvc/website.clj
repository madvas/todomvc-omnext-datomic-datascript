(ns todomvc.website
  (:require [com.stuartsierra.component :as c]
            [bidi.bidi :as b]
            [clojure.java.io :as io]
            [yada.yada :refer [yada] :as y]
            [yada.resources.classpath-resource :as yr]
            [todomvc.parser :as tp]
            [om.next.server :as om]
            [todomvc.util :as u]
            [clojure.walk :as walk]
            [clojure.core.async :refer [>!!]]
            [om.next.impl.parser :as omp]
            [todomvc.queries :as q]))

(def transit #{"application/transit+json"})

(defn mutation? [query]
  (some (comp symbol? :dispatch-key) (:children (omp/query->ast query))))

(defn strip-result [data]
  (walk/postwalk
    (fn [x]
      (if (and (sequential? x) (= :result (first x)))
        [(first x) (dissoc (second x) :db-before :db-after :tx-data)]
        x)) data))

(defn om-next-query-resource [parser c]
  (let [env (select-keys (:db c) [:conn])
        sse-ch (-> c :sse :channel)]
    (y/resource
      {:methods
       {:post
        {:consumes transit
         :produces transit
         :response (fn [ctx]
                     (let [query (:body ctx)
                           res (parser env query)]
                       (if (mutation? query)
                         (let [tx-report (-> res first second :result)
                               mutation (ffirst res)]
                           (>!! sse-ch (u/write-transit
                                         {:tempids (get-in res [mutation :result :tempids])
                                          :tx-data (q/read-tx-data tx-report)}))
                           [])                              ; After mutation we send empty response to client
                         (strip-result res))))}}})))        ; because he will get response as SSE

(defrecord Website []
  c/Lifecycle
  (start [component]
    component)
  (stop [component]
    component)

  b/RouteProvider
  (routes [component]
    ["/" [["api" (yada (om-next-query-resource (om/parser {:read tp/readf :mutate tp/mutatef}) component))]
          ["" (yada (io/resource "public/html/index.html"))]
          ["" (yada (yr/new-classpath-resource "public/"))]]]))

(defn new-website []
  (-> (map->Website {})))