(ns todomvc.server
  (:require [clojure.java.io :as io]
            [clojure.walk :as walk]
            [todomvc.util :as util]
            [ring.util.response :refer [response file-response resource-response]]
            [todomvc.middleware
             :refer [wrap-transit-body wrap-transit-response
                     wrap-transit-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [bidi.bidi :as bidi]
            [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [todomvc.datomic]
            [om.next.server :as om]
            [todomvc.parser :as parser]
            [org.httpkit.server :refer [run-server]]
            [todomvc.util :as u]))

;; =============================================================================
;; Routes

(def routes
  ["" {"/" :index
       "/api"
           {:get  {[""] :api}
            :post {[""] :api}}
       "/chsk"
           {:get  {[""] :chsk-get}
            :post {[""] :chsk-post}}}])

;; =============================================================================
;; Handlers

(defn index [req]
  (assoc (resource-response (str "html/index.html") {:root "public"})
    :headers {"Content-Type" "text/html"}))

(defn generate-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/transit+json"}
   :body    data})

(defn api [req]
  (let [data ((om/parser {:read parser/readf :mutate parser/mutatef})
               {:conn (:datomic-connection req)} (:transit-params req))
        data' (walk/postwalk (fn [x]
                               (if (and (sequential? x) (= :result (first x)))
                                 [(first x) (dissoc (second x) :db-before :db-after :tx-data)]
                                 x))
                             data)]
    (generate-response data')))

;;;; PRIMARY HANDLER

(defn handler [{:keys [ajax-get-or-ws-handshake-fn ajax-post-fn]} req]
  (let [match (bidi/match-route routes (:uri req)
                                :request-method (:request-method req))]
    (case (:handler match)
      :index (index req)
      :api (api req)
      :chsk-get (ajax-get-or-ws-handshake-fn req)
      :chsk-post (ajax-post-fn req)
      req)))

(defn wrap-connection [handler conn]
  (fn [req] (handler (assoc req :datomic-connection conn))))

(defn todomvc-handler [sente conn]
  (-> (partial handler sente)
      (wrap-connection conn)
      wrap-keyword-params
      wrap-params
      wrap-transit-params
      wrap-transit-response
      (wrap-resource "public"))
  )

(defn todomvc-handler-dev [sente conn]
  (fn [req]
    ((todomvc-handler sente conn) req)))

;; =============================================================================
;; WebServer

(defrecord WebServer [port handler container datomic-connection sente]
  component/Lifecycle
  (start [component]
    (let [handler (partial handler sente)
          conn (:connection datomic-connection)]
      (if container
        (let [req-handler (handler conn)
              container (run-server req-handler {:port port})]
          (assoc component :container container))
        ;; if no container
        (assoc component :handler (handler conn)))))
  (stop [component]
    (println "stoping webserver")
    ((:container component))))

(defn dev-server [web-port]
  (WebServer. web-port todomvc-handler-dev true nil nil))

(defn prod-server []
  (WebServer. nil todomvc-handler false nil nil))

;; =============================================================================
;; Route Testing

(comment
  (require '[todomvc.core :as cc])

  (cc/dev-start)

  ;; get todos
  (handler
    {:uri                "/api"
     :request-method     :post
     :transit-params     [{:todos/list [:db/id :todos/created :todo/title :todo/completed]}]
     :datomic-connection (:connection (:db @cc/servlet-system))})

  (.basisT (d/db (:connection (:db @cc/servlet-system))))

  ;; create todo
  (handler {:uri                "/api"
            :request-method     :post
            :transit-params     '[(todos/create {:todo/title "Toilet paper"})]
            :datomic-connection (:connection (:db @cc/servlet-system))})

  ;; run functions first?
  '[(todo/create {:todos/title "New Todo"}) :todos/count]

  {:todo/count  5
   'todo/create {:error "Not logged in"}}
  )
