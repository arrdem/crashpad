(ns crashpad.types
  (:require [guten-tag.core :refer [deftag]]))

(defn maybe? [p]
  (fn [x]
    (or (nil? x)
        (p x))))

(defn bool? [x]
  (instance? Boolean x))



(deftag Lease
  [address
   price
   stove
   washer
   dryer
   ]
  {:pre [(string? address)
         (integer? price)
         ((maybe? bool?) stove)]})
