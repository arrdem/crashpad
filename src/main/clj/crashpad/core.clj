(ns crashpad.core
  (:require [crajure.core :as craj]
            [medley.core :refer :all]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn p [qstr coll]
  (when-not (empty? coll)
    (printf "** %s\n" qstr)
    (doseq [{:keys [title price region url] :as e} coll]
      (printf "*** %s\n    Price: $%s\n    Region: %s\n    Url: %s\n\n"
              title price region url))))

(defn do-query [visited query]
  (let [results  (->> (craj/query-cl query)
                      (filter #(>= 2850 (:price %)))
                      (distinct-by :preview)
                      (distinct-by :title)
                      (remove #(or (contains? visited (:preview %))
                                   (contains? visited (:title %))
                                   (contains? visited (:url %)))))
        visited' (->> (for [r results
                            k [:preview :title :url]]
                        (get r k))
                      (into visited))]
    [visited' results]))

(defn do-queries [visited querries]
  (reduce (fn [[v r] q]
            (let [[v₁ r₁] (do-query v q)]
              [v₁ (into r r₁)]))
          [visited []]
          querries))

(defn ->query [neighborhood]
  {:query   neighborhood
   :area    "sfbay"
   :section :housing/apartments})

(defn -main []
  (let [neighborhoods ["south of market"
                       "alamo square"
                       "western addition"
                       "hayes valley"]
        f             (io/file "crawled-apartments.org")
        g             (io/file "visited.edn")
        _             (if-not (.exists g)
                        (spit g #{}))
        visited       (edn/read-string (slurp g))]
    (with-open [outf (io/writer f :append true)]
      (binding [*out* outf]
        (printf "* Crawl on %s\n" (java.util.Date.)))
      (loop [visited             visited
             [n & neighborhoods] neighborhoods
             acc                 0]
        (if n
          (do (println "Searching in" n)
              (let [[visited' results] (do-query visited (->query n))]
                ;; update output file
                (binding [*out* outf]
                  (p n results))

                (recur visited' neighborhoods (+ acc (count results)))))
          (do
            ;; generate visited file
            (spit (io/writer g) visited)

            (println "Done! found" acc "places :D")))))))
