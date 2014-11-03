(ns sequoia.debug
  (:require [sequoia.delivery :refer :all]))

(assoc-at {:foo {}} [:foo] :name "bob" :age 14)