(ns sequoia.core
  (:require [clj-time.core :as t]
            [clojure.core.async :as a]
            [clojure.java.io :refer [output-stream]]
            [clojure.data.fressian :as f]
            [clojure.data :as d]))

(def sequoia-version "0.1.0")

(defn- pds->dds
  ([pds]
   (pds->dds nil pds false))
  ([previous current]
   (pds->dds previous current false))
  ([previous current deleted]
   (assoc current
     :sequoia/time (t/now)
     :sequoia/previous previous
     :sequoia/deleted? deleted)))

  ;; How to write out parents? Need a way to identify across VMs.  Options:
  ;;
  ;; 1) uuid
  ;; 2) file position

  ;; Use file position
  ;; maintain a map of current dds to file position
  ;; do this while loading as well
  ;; serialize the file position
  ;; remove the file position from the dds?
  ;; file position probably needs to be sent back on a channel

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
  (let [out (output-stream file)]
    {:latest #{}
     :all #{}
     :file file
     :out out
     :writer (f/create-writer out)
     :version sequoia-version}))

(defn load-db
  [file]
  ;; build up map of {position:long object:map}
  ;; reading beginning to end loads old first therefore never have to
  ;; goto disk for a parent

  )

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
  (let [dds (pds->dds pds)]
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
  (let [dds (pds->dds previous current)]
    (->
     db
     (save! (changes previous dds))
     (update-in [:latest] disj previous)
     (update-in [:latest] conj dds)
     (update-in [:all] conj dds))))

(defn delete!
  [db current]
  (let [dds (pds->dds current current true)]
    (->
     db
     (save! (changes current dds))
     (update-in [:latest] disj current)
     (update-in [:all] conj dds))))

(defn history [dds]
  (let [parent (dds :previous)]
    (if parent
      (cons parent (lazy-seq (history parent)))
      nil)))

