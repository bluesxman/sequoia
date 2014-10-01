(ns sequoia.core2
  (:require [clj-time.core :as t]
            [clj-tuple :refer [tuple]]))

(defn begin!
  "Creates a new chronicle"
  ([file])
  ([file timefn]))

(defn load!
  "Loads an existing chronicle"
  [file])

(defn update!
  ([[chron prev] now])
  ([chron prev now]))

(defn merge!
  ([[chron prev] change]
   (merge! chron prev change))
  ([chron prev change]
   (let [cur (merge prev change)]
     [(update! chron prev cur) cur])))

(defn into!
  [chron prev changes]
  (reduce merge! [chron prev] changes))

(defn create!
  ([[chron _] now])
  ([chron now])
  ([chron initial changes]
   (into! (create! chron initial) initial changes)))

(defn delete!
  [chron entity])

(defn create-each!
  [chron cs]
  (reduce create! chron cs))

(defn create-by!
  ([timefn chron cur])
  ([timefn chron initial changes]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Delivery
;;
;; 1) Package 1 from Amazon arrives at Warehouse 5 in Denver, CO
;; 2) Package 2 from EBay arrives at Warehouse 5 in Denver, CO
;; 3) Package 3 from Newegg arrives at Warehouse 6 in Boulder, CO
;; 4) Package 2 then 1 are loaded onto Truck 12
;; 5) Package 3 is loaded onto Truck 17
;; 6) Package 2 is delivered to 123 Foo Rd and signed for by Jay
;; 7) Package 3 is delivered to 789 Meh Ln and signed for by Bob
;; 8) Package 1 is delivered to 456 Blah Ave and signed for by Bev
;;
;; Select Truck 12 when it contained just one package


;; Time-series data
;;
;; 1) For each truck, merge time-position data for the day
;;
;; How much time did trucks spend above y = 50?


(->
 (begin! "example.frs")
 (create! {:id 5 :city "Denver" :state "CO"})
 (create! {:id 6 :city "Boulder" :state "CO"})
 (create! {:id 1 :from "Amazon" :to "123 Foo Rd"})
 (create! {:id 2 :from "EBay" :to "789 Meh Ln"})
 (create! {:id 3 :from "Newegg" :to "456 Blah Ave"}))

(let [wh5 {:type :warehouse :id 5 :city "Denver" :state "CO"}
      wh6 {:type :warehouse :id 6 :city "Boulder" :state "CO"}
      pkg1 {:type :package :id 1 :from "Amazon" :to "123 Foo Rd"}
      pkg2 {:type :package :id 2 :from "EBay" :to "789 Meh Ln"}
      pkg3 {:type :package :id 3 :from "Newegg" :to "456 Blah Ave"}
      trk12 {:type :truck :id 12}
      trk17 {:type :truck :id 17}])
