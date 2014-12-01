(ns sequoia.proto
  (:import (java.io IOException))
  (:require [clj-time.core :as t]))

;; TODO: make a sequoia.id namespace

(defn- long-ider
  ([idatom m]
   (assoc m :sequoia/id (swap! idatom inc)))
  ([idatom m & ms]
   (cons (long-ider idatom m) (map #(long-ider idatom %) ms))))

(defn long-identifier
  "Returns a function which will identify a map.  A :sequoia/id
  key is added to the map with a unique number for the value."
  ([]
   (long-identifier -1))
  ([seed]
    (partial long-ider (atom seed))))

;; Is 'add' necessary when there 'commit'?
;;
;; Advantages of add
;; Can check for whether an id already exists
;;
;; Advantages of commit only
;; For adding information to a journal, one function is more simple than two

(defn add
  ([j m]
   j)
  ([j m & ms]
   j))

(defn- save-edn
  [out edn]
  )

(defn rollback
  [io])

(defn mark
  [io])

(defn log-err
  [e]
  (.printStackTrace e))

(defn commit
  "Commits one or more identified maps to the journal.  Returns
  the new journal once all changes have been written to durable
  storage."
  ([j {id :sequoia/id :as m}]
   (-> (if id
         (assoc j :entities id m)
         )
       (assoc :commit-time (t/now))))
  ([j m & ms]
   (let [start-time (t/now)
         {out :out} j]
     (try
       (do
         (mark out))
       (catch IOException e
         (do
           (rollback out)
           (log-err e)))))))

(defn save-long-identifier
  [j ider]
  (commit j {assoc (ider {}) :sequoia/long-ider nil}))

;; THOUGHT:  The identifier should be kept completely separate from the database.
;; Persisting the sate of an identifier (should if have any) is a separate
;; concern.
;;
;; COUNTERPOINT:  If the identifier state is saved atomically with the new journal
;; then the identifier state is not durable, *if* something about the identifier state
;; needs to be persisted.  For example, the long-identifier *could* find max id from
;; all ids in the database.  This might not be fast, unless on loads there could be some
;; sort of callback that could be invoked as entities are loaded

(def ider (long-identifier 1))

(def states1
  [{:name "bob" :pos [5 10] :time "5:00"}
   {:name "bob" :pos [5 10] :time "5:01"}
   {:name "jane" :pos [4 7] :time "5:01"}
   {:name "jane" :pos [4 7] :time "5:02"}])

(def states2
  [{:name "bob" :pos [1 9] :time "5:03"}
   {:name "jane" :pos [4 7] :time "5:03"}])

(defn id-if
  [ider entity]
  (if (contains? entity :sequoia/id)
    entity
    (ider entity)))

(def people-lookup (atom {}))

(defn id-by
  [ider lookup keys entity]
  (let [lookup-key (apply entity keys)
        id (@lookup lookup-key)]
    (if id
      (assoc entity :sequoia/id id)
      (let [identified (ider entity)]
        (swap! lookup assoc lookup-key (identified :sequoia/id))
        identified))))

(def ider2 (partial id-by ider people-lookup [:name]))

(map ider2 states1)

(defn journal
  []
  {:entities {} :prev nil})

(def jrn (journal))
(partition-by :time states1)

(defn index
  [maps k]
  (loop [idx {}
         ms maps]
    (if (empty? ms)
      idx
      (let [m (first ms)
            v (k m)
            nxt (if (contains? idx v)
                  (update-in idx [v] conj m)
                  (assoc idx v [m]))]
        (recur nxt (rest ms))))))

(index states1 :time)
