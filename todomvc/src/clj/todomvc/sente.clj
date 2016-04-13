(ns todomvc.sente
  (:require [com.stuartsierra.component :as component]
            [taoensso.sente :as sen]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [taoensso.sente.packers.transit :as sen-transit]
            [cognitect.transit :as transit]
            [todomvc.util :as u]
            [clojure.walk :as walk]
            [todomvc.parser :as parser]
            [om.next.server :as om])
  (:import [org.joda.time DateTime ReadableInstant]))

; This whole file isn't at use currently, ignore it!

(defn api [req params]
  (let [data ((om/parser {:read parser/readf :mutate parser/mutatef})
               {:conn (:datomic-connection req)} params)
        data' (walk/postwalk (fn [x]
                               (if (and (sequential? x) (= :result (first x)))
                                 [(first x) (dissoc (second x) :db-before :db-after :tx-data)]
                                 x))
                             data)]
    data'))

(def joda-time-writer
  (transit/write-handler
    (constantly "m")
    (fn [v] (-> ^ReadableInstant v .getMillis))
    (fn [v] (-> ^ReadableInstant v .getMillis .toString))))

(defn recv-handler [sente {:keys [event id ?data send-fn ?reply-fn uid ring-req client-id
                                  connected-uids] :as p} & r]
  (let [chsk-send! (:send-fn sente)
        [evt-id params] event]
    (u/p "RECEIVER: " ?reply-fn evt-id " " params)
    (if (= evt-id :todomvc/api)
      (let [resp (api ring-req params)]
        (u/p "response: " resp)
        (chsk-send! uid [:todomvc/api resp]))
      (chsk-send! uid [:todomvc/api {:abc "def"}]))))

(defrecord Sente []
  component/Lifecycle
  (start [component]
    (let [sente (sen/make-channel-socket-server! sente-web-server-adapter
                  {:packer (sen-transit/->TransitPacker :json {:handlers {DateTime joda-time-writer}} {})})
          recv-loop (sen/start-chsk-router! (:ch-recv sente) (partial recv-handler sente))]

      (merge component sente {:recv-loop recv-loop})))
  (stop [{:keys [recv-loop]}]
    (recv-loop)))

(defn new-sente []
  (Sente.))
