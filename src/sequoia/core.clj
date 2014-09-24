(ns sequoia.core
  (:require [clj-time.core :as t]
            [clojure.core.async :as a]
            [clojure.java.io :refer [output-stream]]
            [clojure.data.fressian :as f]
            [clojure.data :as d]))

(def sequoia-version "0.1.0")

(defn- gen-tx
  ([]
   (gen-tx nil false))
  ([previous]
   (gen-tx previous false))
  ([previous deleted]
   (gen-tx previous deleted (t/now)))
  ([previous deleted time]
   {:time time
    :previous previous
    :deleted? deleted}))

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
    {:latest {}
     :all {}
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
  [db pds tx]
  ;; impl
  db)

(defn create!
  [db pds]
  (let [tx (gen-tx)]
    (->
     db
     (save! pds tx)
     (update-in [:latest] assoc pds tx)
     (update-in [:all] assoc pds tx))))


(second (d/diff {:name "bob" :age 21}
         {:name "bob" :age 22 :job "Chef"}))
(->
 (new-db! "foo.frs")
 (create! {:name "bob" :age 21})
 (update! {:name "bob" :age 21}
          {:name "bob" :age 22 :job "Chef"}))


(defn- changes
  [previous current]
  (second (d/diff previous current)))

(defn update!
  [db previous current]
  (let [tx (gen-tx previous)]
    (->
     db
     (save! (changes previous current) tx)
     (update-in [:latest] dissoc previous)
     (update-in [:latest] assoc current tx)
     (update-in [:all] assoc current tx))))

;; ??? using the pds as the key means that a delete erases history
;; Idea: use a deleted set where :all does not contain :deleted

(defn delete!
  [db current]
  (let [tx (gen-tx current true)]
    (->
     db
     (save! {} tx)
     (update-in [:latest] dissoc current)
     (update-in [:all] assoc current tx))))

(defn history [db pds]
  (let [parent (get-in db [:all pds])]
    (if parent
      (cons parent (lazy-seq (history db parent)))
      nil)))

