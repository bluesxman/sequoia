(ns sequoia.core
  (:require [clojure.java.io :as io]
            [clojure.data.fressian :as f]
            [clojure.data :as d]))


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
