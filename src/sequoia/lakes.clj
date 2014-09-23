(ns sequoia.lakes
  (:require [clojure.java.io :as io]
            [clojure.data.fressian :as f]
            [clojure.data :as d]
            [clj-time.format :refer [formatter parse]]
            [clojure.string :refer [split split-lines]]
            [sequoia.core :as sq]))

(def uri "ftp://ftp.usbr.gov/uc/agilmore/elpaso/ELEPHANTBUTTEDAM.dat")

(defn valid?
  [row]
  (= 4 (count row)))

;; http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html
(def usbr-time-format (clj-time.format/formatter "YYYY-MM-ddHH:mm:sszzz"))

(defn row->obj [time-zone cells]
  (let [[date time elev vol] cells]
    {:time (clj-time.format/parse usbr-time-format (str date time time-zone))
     :elevation (read-string elev)
     :volume (read-string vol)}))

(defn uri->levels
  [db uri]
  (->>
   (slurp uri)
   (split-lines)
   (drop 5)
   (map #(split % #"\s+"))
   (filter valid?)
   (map #(row->obj "MDT" %))
;;    (reduce #(create! %) db)
;;    (add-history! db)
   ))


(take-last 5 elephant)


(def lakes
  [{:name "Elephant Butte"
    :level-uri "ftp://ftp.usbr.gov/uc/agilmore/elpaso/ELEPHANTBUTTEDAM.dat"
    :state "NM"
    :county "Sierra"
    :zip-code 87935
    :area-code 575
    :latitude 33.189722
    :longitude -107.222778
    :time-zone "MDT"}

   {:name "Caballo"
    :level-uri "ftp://ftp.usbr.gov/uc/agilmore/elpaso/CABALLODAM.dat"
    :state "NM"
    :county "Sierra"
    :area-code 575
    :latitude 32.92568
    :longitude -107.2961
    :time-zone "MDT"}])

(def db
  (->
   (sq/new-db! "nm-lakes.frs")
   #(reduce sq/create! % lakes)
   ))

(def db
  (->
   lakes
   ))


(defn add-lake
  [db lake]
  (->
   db
   (sq/create! lake)
   #(reduce sq/update)))

(with-db db
  (->
   lakes
   (sq/create!)
   ()))

(def base {:name "foo"})
(def states [{:state 1} {:state 2} {:state 3} {:state 4}])

(reduce merge base states)

(->
 db
 (sq/create! base)
 (sq/update! base states)) ;;

(defn new-state [db lake state]
  (->>
   (merge lake state)
   (sq/update! db lake)))

(reduce #(new-state %))

(loop [db database
       cur lake
       s states]
  (if s
    (let [nxt (merge cur (first s))]
      (recur (sq/update! db cur nxt) nxt (rest s)))
    db))

(sq/create! db lake)
(sq/update! db )

(defn merge-each [current changes]
  (if (empty? changes)
    nil
    (let [nxt (merge current (first changes))]
      (cons nxt (lazy-seq (merge-each nxt (rest changes)))))))

(merge-each base states)
(defn create!
  [db obj & history])

;; 1) create db
;; 2) create each lake, producing a new db with a dds for each lake
;; 3) for each lake, update it with each level data update, producing a new db


;; 2) create lake
;; 3) add state data
;; 4) load copy of db
;; 5) test for equality


;; 1) create many from seq
;; 2) create one from seq
;; 3) create one from one
;; 4) update one from many
;; 5) update one from one

;; 1:  (apply create! db seq)
;; 2:  (update! db (create! db (first seq)) (rest seq))
;; 2:  (apply update! db (create! db (first seq)) (rest seq))
;; 2:  (create-history! db seq)
;; 3:  (create! db obj)
;; 4:  (update! db prev seq)
;; 4:  (apply update! db prev seq)
;; 5:  (update! db prev cur)
