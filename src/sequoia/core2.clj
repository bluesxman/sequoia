(ns sequoia.core2
  (:require [clj-time.core :as t]
            [clj-tuple :refer [tuple]]))

(defn new-journal!
  [file])

(defn load-journal!
  [file])

(defn create!
  [db entity])

(defn update!
  ([[db prev] change])
  ([db prev change]))

(defn delete!
  [db entity])

(def create!
  ([db cur])
  ([db initial changes]
   (into! (create! db initial) initial changes)))

(def create-each!
  [db cs]
  (reduce create! db cs))

(def create-by!
  ([timefn db cur])
  ([timefn db initial changes]))

(def merge!
  ([[db prev] change]
   (merge! db prev change))
  ([db prev change]
   (let [cur (merge prev change)]
     [(update! db prev cur) cur])))

(def into!
  [db prev changes]
  (reduce merge! [db prev] changes))
