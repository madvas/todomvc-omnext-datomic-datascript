(ns todomvc.util
  (:require [cognitect.transit :as t]
            [cljs.pprint :refer [pprint]]
            [om.transit :as omt])
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

(defn transit-post [url]
  (fn [{:keys [remote]} cb]
    (.send XhrIo url
           (fn [e]
             (this-as this
               (cb (t/read (omt/reader) (.getResponseText this)))))
           "POST" (t/write (omt/writer) remote)
           #js {"Content-Type" "application/transit+json"})
    ))

(defn prevent-default [e]
  (doto e (.preventDefault) (.stopPropagation)))

(defn target-val [e]
  (.. e -target -value))

(def ESCAPE_KEY 27)
(def ENTER_KEY 13)


(defn on-key-down [c props key-fns]
  (fn [e]
    (let [f (condp == (.-keyCode e)
              ESCAPE_KEY (:escape-key key-fns)
              ENTER_KEY (:enter-key key-fns)
              #(do %))]
      (f c props e)
      )))

(defn sort-by-dir [keyfn dir coll]
  (let [comp (if (= dir :desc) #(compare %2 %1) #(compare %1 %2))]
    (sort-by keyfn comp coll)))

(defn submap? [sub orig]
  (= sub (select-keys orig (keys sub))))

(comment
  (def sel [{:todos/list [:db/id :todo/title :todo/completed :todo/created]}])

  (t/write (t/writer :json) sel)
  )

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

(defn eq-in-key? [k & ms]
  (-> (map k ms) set count (= 1)))