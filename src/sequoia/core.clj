(ns sequoia.core
  (:require [clj-time.core :as t]))

(defn add-meta
  ([pds]
   (add-meta nil pds false))
  ([previous current]
   (add-meta previous current false))
  ([previous current deleted]
   (assoc current
     :time (t/now)
     :parent previous
     :deleted? deleted)))

(defn new-db
  []
  {:latest #{}
   :all #{}})

(defn create
  [db pds]
  (let [dds (add-meta pds)]
    (->
     db
     (update-in [:latest] conj dds)
     (update-in [:all] conj dds))))

(defn update
  [db previous current]
  (let [dds (add-meta previous current)]
    (->
     db
     (update-in [:latest] disj previous)
     (update-in [:latest] conj dds)
     (update-in [:all] conj dds))))

(defn delete
  [db current]
  (let [dds (add-meta current current true)]
    (->
     db
     (update-in [:latest] disj current)
     (update-in [:all] conj dds))))

(defn history [dds]
  (let [parent (dds :parent)]
    (if parent
      (cons parent (lazy-seq (history parent)))
      nil)))

