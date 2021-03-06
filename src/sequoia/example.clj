(ns sequoia.example
  (:require [clojure.java.io :as io]
            [clojure.data.fressian :as f]
            [clojure.data :as d]
            [clj-time.format :refer [formatter parse]]
            [clojure.string :refer [split split-lines]]))

(def file "people.frs")

(def data [{:name "bob" :age 34}
           {:name "jill" :age 29}])

(def out (io/output-stream file))
(def wrt (f/create-writer out))

(doseq [p data]
  (.writeObject wrt p))

(.flush out)
(.close wrt)

(def in (io/input-stream file))
(def rdr (f/create-reader in))

(def bob (f/read-object rdr))

(eval bob)

(def jill (f/read-object rdr))

(eval jill)

(eval in)

(merge jill bob)

(time (merge bob jill))

(time (reduce #(conj %1 %2) bob jill))

(d/diff {:foo 1 :blah 2} {:foo 1 :blah 3 :meh 5})



(def uri "ftp://ftp.usbr.gov/uc/agilmore/elpaso/ELEPHANTBUTTEDAM.dat")
(def raw (slurp "ftp://ftp.usbr.gov/uc/agilmore/elpaso/ELEPHANTBUTTEDAM.dat"))

(eval raw)

(clojure.string/split "2014-09-11 00:00:00 4305.21 156905" #"\s")

(map #(clojure.string/split % #"\s+") (drop 5 (clojure.string/split-lines raw)))

;; http://joda-time.sourceforge.net/apidocs/org/joda/time/format/DateTimeFormat.html

(def usbr-time-format (clj-time.format/formatter "YYYY-MM-ddHH:mm:sszzz"))

(defn row->obj [time-zone cells]
  (let [[date time elev vol] cells]
    {:time (clj-time.format/parse usbr-time-format (str date time time-zone))
     :elevation (read-string elev)
     :volume (read-string vol)}))

(row->obj "MDT" ["2014-09-11" "00:00:00" "4305.21" "156905"])

(defn valid?
  [row]
  (= 4 (count row)))

(defn uri-into-db
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
   )) ;; what's an elegant solution to composability for create/update for a history of a single thing???

(->>
   raw
   (split-lines)
   (drop 5)
   (map #(split % #"\s+"))
   (filter valid?)
   (map #(row->obj "MDT" %))
   )

(def elephant (uri-into-db nil uri))

(take 5 elephant)

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

;; 1) create db
;; 2) create lake
;; 3) add state data
;; 4) load copy of db
;; 5) test for equality



(sq/merge! [db lake] change)
(reduce sq/merge! [db lake] changes)

;; update: map -> map -> map -> map
;; update: database previous current new-database

;; merge: map -> map -> map -> vector map
;; merge: database previous change [new-database current]

;; into: map -> map -> seq map -> vector map
;; into: database previous changes [new-database current]

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

(def db
  {:timefn (let [time (t/now)] (fn [pds] time))
   :previous
   :deleted?
   :uuid})

;; create! =
;; update! = declare
;; merge! = change
;; into = change-all
