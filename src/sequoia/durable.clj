(ns sequoia.durable
  (:import (java.io BufferedOutputStream ByteArrayOutputStream ByteArrayInputStream))
  (:require [clojure.data.fressian :as f]))

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