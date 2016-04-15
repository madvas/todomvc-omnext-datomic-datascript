(ns todomvc.util
  (:require [cognitect.transit :as t]
            [cljs.pprint :refer [pprint]]
            [om.transit :as omt]
            [goog.events.KeyCodes :as kc])
  (:import [goog.net XhrIo]))


(defn p [& args]
  "Like print, but returns last arg. For debugging purposes"
  (doseq [a args]
    (let [f (if (map? a) pprint print)]
      (f a)))
  (println)
  (flush)
  (last args))

(defn pcoll [items]
  (doall (map p items)))

(defn hidden [is-hidden]
  (if is-hidden
    #js {:display "none"}
    #js {}))

(defn pluralize [n word]
  (if (== n 1)
    word
    (str word "s")))

(defn read-transit [x]
  (t/read (omt/reader) x))

(defn transit-post [url]
  (fn [{:keys [remote]} cb]
    (.send XhrIo url
           (fn [e]
             (this-as this
               (cb (read-transit (.getResponseText this)))))
           "POST" (t/write (omt/writer) remote)
           #js {"Content-Type" "application/transit+json"})))

(defn prevent-default [e]
  (doto e (.preventDefault) (.stopPropagation)))

(defn target-val [e]
  (.. e -target -value))

(defn on-key-down [key-fns]
  (fn [e]
    (let [f (condp == (.-keyCode e)
              kc/ESC (:escape-key key-fns)
              kc/ENTER (:enter-key key-fns)
              #(do %))]
      (f e))))

(defn sort-by-dir [keyfn dir coll]
  (let [comp (if (= dir :desc) #(compare %2 %1) #(compare %1 %2))]
    (sort-by keyfn comp coll)))

(defn event-data [e]
  (-> e (aget "event_") (aget "data")))

(defn apply-if [pred f x & args]
  (if-not (pred x)
    (apply f x args)
    x))
