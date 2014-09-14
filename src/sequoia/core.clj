(ns sequoia.core
  (:require [clj-time.core :as t]
            [clojure.core.async :as a]
            [clojure.java.io :refer [writer]]))

(def sequoia-version 0.1)

(defn- add-meta
  ([pds]
   (add-meta nil pds false))
  ([previous current]
   (add-meta previous current false))
  ([previous current deleted]
   (with-meta
     (assoc current
       :time (t/now)
       :parent previous
       :deleted? deleted)
     {:sequoia sequoia-version})))

(defn- write-dds
  [out dds]
  (.write (str dds "\n")))

(defn- init-writer!
  [file chan]
  (let [out (writer file)
        run (fn []
              (loop [msg (a/<!! chan)]
                (if (or (= msg :shutdown) (nil? msg))
                  nil
                  (do
                    (write-dds out msg)
                    (recur (a/<!! chan))))))]
    (future run)))

(defn new-db!
  [file]
  (let [io (a/chan)]
    (init-writer! file io)
    {:latest #{}
     :all #{}
     :file file
     :io io}))

(defn load-db
  [file])

(defn annihilate!
  "Obliterate any and all history prior to time 't'"
  [db t])

(defn shutdown!
  [db]
  (a/>!! (db :io) :shutdown)
  (a/close! (db :io))
  db)

;; (a/>!! (a/chan) :shutdown)

(defn restart!
  [db]
  (->
   (shutdown! db)
   (load-db (db :file))))

(defn- save!
  [db dds]
  (a/>!! (db :io) dds)
  db)

(defn create!
  [db pds]
  (let [dds (add-meta pds)]
    (->
     db
     (save! dds)
     (update-in [:latest] conj dds)
     (update-in [:all] conj dds))))

(defn update!
  [db previous current]
  (let [dds (add-meta previous current)]
    (->
     db
     (save! dds)
     (update-in [:latest] disj previous)
     (update-in [:latest] conj dds)
     (update-in [:all] conj dds))))

(defn delete!
  [db current]
  (let [dds (add-meta current current true)]
    (->
     db
     (save! dds)
     (update-in [:latest] disj current)
     (update-in [:all] conj dds))))

(defn history [dds]
  (let [parent (dds :parent)]
    (if parent
      (cons parent (lazy-seq (history parent)))
      nil)))

