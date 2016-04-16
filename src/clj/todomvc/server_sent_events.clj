(ns todomvc.server-sent-events
  (:require [clojure.core.async :as a]
            [com.stuartsierra.component :as c]
            [clojure.core.async :refer [go go-loop timeout <!! >!! chan close!] :as a]
            [bidi.bidi :as b]
            [yada.yada :refer [yada] :as y]
            [todomvc.util :as u]
            [todomvc.queries :as q]))

(defn new-handler [mlt]
  (yada (y/resource {:methods {:get {:produces "text/event-stream"
                                     :response mlt}}})))

(defrecord ServerSentEvents []
  c/Lifecycle
  (start [component]
    (assoc component :channel (chan 10)))
  (stop [component]
    (when-let [ch (:channel component)]
      (close! ch))
    component)

  b/RouteProvider
  (routes [component]
    ["/events" (-> (new-handler (a/mult (:channel component)))
                   (b/tag ::events))]))

(defn new-server-sent-events []
  (map->ServerSentEvents {}))