(ns todomvc.util
  (:require [datomic.api :as d]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.pprint :refer [pprint]]
            [cognitect.transit :as transit]
            [om.transit :as omt])
  (:import [java.io ByteArrayOutputStream]))

(defn p [& args]
  "Like print, but returns last arg. For debugging purposes"
  (doseq [a args]
    (let [f (if (map? a) pprint print)]
      (f a)))
  (println)
  (flush)
  (last args))

(defn write-transit
  ([x] (write-transit x {}))
  ([x opts]
   (let [baos (ByteArrayOutputStream.)
         w (omt/writer baos opts)
         _ (transit/write w x)
         ret (.toString baos)]
     (.reset baos)
     ret)))

(defn s->int [s]
  (if (number? s)
    s
    (Integer/parseInt (re-find #"\A-?\d+" s))))