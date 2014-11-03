(ns sequoia.sequoia.core3
  (:require [clj-time.core :as t]))

(defn create-db
  [file]
  (let [now (t/now)]
    {:prev    nil
     :time now
     :history (sorted-map)
     :ids {}
     :offset 0}))

(defrecord Identity [:uid])
(def ids (atom 0))
(defn- gen-id []
  (Identity. (swap! ids inc)))

(defn conj-in
  ([map path value]
   (update-in map path conj value))

  ([map path value & vs]
   (if vs
     (update-in map path #(apply conj % value vs)))))

(defn create
  [db id col]
  (-> db
      (assoc-in [:ids id] col)
      (conj-in [:history] db)
      (assoc :time t/now :prev db)))

(defn change
  ([db path value])
  ([db p v & pvs]))