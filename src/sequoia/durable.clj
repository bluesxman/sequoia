(ns sequoia.durable
  (:import (java.io BufferedOutputStream ByteArrayOutputStream ByteArrayInputStream))
  (:require [clojure.data.fressian :as f]
            [clojure.data :as d]
            [clojure.set :as set]))

(def db0
  {:smiths {:address "123 Elm Street"
            :occupants [:ben :jan :bentley]}})

(def buf (f/write db0))
(f/read buf)

(def out (ByteArrayOutputStream. 10000))
(def wrt (f/create-writer out))

(f/write-object wrt db0)

(eval out)

(def rdr (f/create-reader (ByteArrayInputStream. (.toByteArray out))))
(f/read-object rdr)

;; By using the FileChannel for a FileInputStream, the channel's
;; position methods can be used to control the position in the file.
;; Both a read and write channel should be open at the same time.
;; Reads could probably be multi-threaded with a reader per thread

(def a {:a 1 :b 2 :c #{3 4} :d '(6 7)})

(def b (-> a
           (dissoc :a)
           (update-in [:c] conj 5)
           (update-in [:d] rest)))

(d/diff a b)

(d/diff [1 2] [1 2 3])

(d/diff '(7) '(7 6))

(set/difference #{2 4} #{1 2 3})

(defn- write-all
  [wrt op k v & vs])

;; types to diff
;; map, set, list, vector, all else
(defn diff
  "calculates the sequence of operations to record the changes"
  [prev cur]
  (if (= (type prev) (type cur))
    ()
    cur))

(defn write
  "Write "
  [wrt db k]
  (let [[prev cur _] (d/diff (k (:prev db)) (k db))]
    (if prev (write-all wrt :dissoc (keys prev)))
    (if cur (write-all wrt :assoc ))))


