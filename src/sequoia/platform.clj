(ns sequoia.platform)

;; Handle writes of journal to storage
;; Handle logging
;; Handle time (clj-time wont handle everything)

(def pf :jvm)

(defn timestamp
  []
  (case pf
    :jvm
    :js-node
    :js-browser
    :clr
    :android))

