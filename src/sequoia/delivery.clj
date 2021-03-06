(ns sequoia.delivery)

; Delivery
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


(defn conj-in
  ([map path value]
   (update-in map path conj value))

  ([map path value & vs]
   (if vs
     (update-in map path #(apply conj % value vs)))))

(defn add-in
  ([map path default value]
   (update-in map path #(conj (if (nil? %) default %) value)))
  ([map path default value & vs]
   (update-in map path #(apply conj (if (nil? %) default %) value vs))))

(defn disj-in
  [map path value]
  (update-in map path disj value))

(defn dissoc-in
  [map path key]
  ())

(defn assoc-at
  "Associate all key-value pairs with a map/vector at a location specified by
  the path."
  [map [p & ps] & kvs]
  (if p
    (assoc map p (apply assoc-at (map p) [ps] kvs))
    (apply assoc map kvs)))

(defn db-when
  "Returns the value of the database at a time in its history"
  [db time]
  (first (subseq (:history db) <= time)))

(defrecord Identity [uuid])

(defn id?
  "Tests whether x is a Sequoia identity"
  [x]
  (instance? Identity x))

(defn fetch-in
  "Takes the value of the database at an instant in time and a path and returns
  the value at that path.  For identities in the path, "
  [db path]
  (loop [[k & ks] path
         cur-val db]
    (if k
      (if (id? k)
        (recur ks (db k))
        (recur ks (cur-val k)))
      cur-val)))

;; db keeps ref to prev db and to a sorted-map by time of all previous dbs

(def wh5 {:type :warehouse :id 5 :city "Denver" :state "CO"})
(def wh6 {:type :warehouse :id 6 :city "Boulder" :state "CO"})
(def pkg1 {:type :package :id 1 :from "Amazon" :to "123 Foo Rd"})
(def pkg2 {:type :package :id 2 :from "EBay" :to "789 Meh Ln"})
(def pkg3 {:type :package :id 3 :from "Newegg" :to "456 Blah Ave"})
(def trk12 {:type :truck :id 12})
(def trk17 {:type :truck :id 17})

(def db0
  {:wh5 wh5
   :wh6 wh6
   :pkg1 pkg1
   :pkg2 pkg2
   :pkg3 pkg3
   :trk12 trk12
   :trk17 trk17})

(def db1 (assoc-in db0 [:wh5 :packages] #{:pkg1}))

(def db2 (conj-in db1 [:wh5 :packages] :pkg2))

(def db3 (assoc-in db2 [:wh6 :packages] #{:pkg3}))

(def db4a
  (->
    db3
    (assoc-in [:trk12 :packages] #{:pkg2})
    (disj-in [:wh5 :packages] :pkg2)))

(def db4b
  (->
    db4a
    (conj-in [:trk12 :packages] :pkg1)
    (disj-in [:wh5 :packages] :pkg1)))

(def db5
  (->
    db4b
    (assoc-in [:trk17 :packages] #{:pkg3})
    (disj-in [:wh6 :packages] :pkg3)))

(def db6
  (->
    db5
    (disj-in [:trk12 :packages] :pkg2)
    (assoc-at [:pkg2] :signed "Jay" :delivered true)))

(def db7
  (->
    db6
    (disj-in [:trk17 :packages] :pkg3)
    (assoc-at [:pkg3] :signed "Bob" :delivered true)))

(def db8
  (->
    db7
    (disj-in [:trk12 :packages] :pkg1)
    (assoc-at [:pkg1] :signed "Bev" :delivered true)))

(filter #(= 1 (count (get-in [:packages] %))) (vals db3))

{:austin {:state "Texas" :population 500000}
 :jan {:age 20 :birthplace :austin}
 :doug {:age 23 :birthplace :austin}}