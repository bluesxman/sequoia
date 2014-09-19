(ns sequoia.core
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

(defn uri-into-db
  [db uri]
  (->>
   (slurp uri)
   (split-lines)
   (drop 5)
   (map #(split % #"\s+"))
   (map row->obj "MDT")
;;    (reduce #(create! %) db)
;;    (add-history! db)
   )) ;; what's an elegant solution to composability for create/update for a history of a single thing???

(->>
   raw
   (split-lines)
   (drop 5)
   (map #(split % #"\s+"))
   (remove #(= 4 (count %)))
   (map #(row->obj "MDT" %))
   )

(uri-into-db nil uri)

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

