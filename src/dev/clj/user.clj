(ns user
  (:require [crajure.core :as craj]
            [medley.core :refer :all]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn p [coll]
  (when-not (empty? coll)
    (println "\n* Apartments " (java.util.Date.))
    (doseq [{:keys [title price region url] :as e} coll]
      (printf "** %s\n   Price: $%s\n   Region: %s\n   Url: %s\n\n"
              title price region url))))

(defn -main []
  (let [f       (io/file "/home/arrdem/Dropbox/crawled-apartments.org")
        g       (io/file "/home/arrdem/Dropbox/visited.edn")
        visited (edn/read-string (slurp g))]
    (with-open [outf (io/writer f :append true)]
      (let [results (->> {:query   ""
                          :area    "sfbay"
                          :section :housing/apartments}
                         craj/query-cl
                         (filter #(>= 2850 (:price %)))
                         (distinct-by :preview)
                         (distinct-by :title)
                         (remove #(or (contains? visited (:preview %))
                                      (contains? visited (:title %))
                                      (contains? visited (:url %)))))]

        ;; generate visited file
        (->> (for [r results
                   k [:preview :title :url]]
               (get r k))
             (into visited)
             (spit (io/writer g)))

        ;; update output file
        (binding [*out* outf]
          (p results))))))
