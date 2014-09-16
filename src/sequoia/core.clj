(ns sequoia.core
  (:require [clj-time.core :as t]
            [clojure.core.async :as a]
            [clojure.java.io :refer [output-stream]]
            [clojure.data.fressian :as f]
            [clojure.data :as d]))

(def sequoia-version "0.1.0")

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
  [wrt out dds]
  (.writeObject wrt dds)
  (.flush out))

(defn- init-writer!
  [file chan]
  (let [out (output-stream file)
        wrt (f/create-writer out)
        run (fn []
              (loop [msg (a/<!! chan)]
                (if (or (= msg :shutdown) (nil? msg))
                  (do
                    (.flush out)
                    (.close wrt)
                    nil)
                  (do
                    (write-dds wrt out msg)
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

(defn- changes
  [previous current]
  ((d/diff previous current) 1))

(defn update!
  [db previous current]
  (let [dds (add-meta previous current)]
    (->
     db
     (save! (changes previous dds))
     (update-in [:latest] disj previous)
     (update-in [:latest] conj dds)
     (update-in [:all] conj dds))))

(defn delete!
  [db current]
  (let [dds (add-meta current current true)]
    (->
     db
     (save! (changes current dds))
     (update-in [:latest] disj current)
     (update-in [:all] conj dds))))

(defn history [dds]
  (let [parent (dds :parent)]
    (if parent
      (cons parent (lazy-seq (history parent)))
      nil)))

