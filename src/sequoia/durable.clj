(ns sequoia.durable
  (:import (java.io BufferedOutputStream ByteArrayOutputStream))
  (:require [clojure.data.fressian :as f]))

(def db0
  {:smiths {:address "123 Elm Street"
            :occupants [:ben :jan :bentley]}})

(def buf (f/write db0))

(f/read buf)

(f/create-writer (ByteArrayOutputStream. (* 1024 1024)))