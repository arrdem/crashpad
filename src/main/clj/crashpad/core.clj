(ns crashpad.core
  (:require [crajure.core :as craj]
            [medley.core :refer :all]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def regions-blacklist
  #{"sonoma"
    "redwood city"
    "portola valley"
    "mill valley"
    "palo alto"
    "san jose south"
    "campbell"
    "san jose north"
    "gilroy"
    "berkeley"
    "santa clara"
    "alameda"
    "fairfield / vacaville"
    "san rafael"
    "albany / el cerrito"
    "oakland north / temescal"
    "mountain view"
    "concord / pleasant hill / martinez"
    "novato"
    "willow glen / cambrian"
    "cole valley / ashbury hts"
    "dublin / pleasanton / livermore"
    "los gatos"
    "berkeley north / hills"
    "san leandro"
    "cupertino"
    "san jose west"
    "san carlos"
    "santa rosa"
    "~san francisco"
    "milpitas"
    "cottonwood"
    "san jose downtown"
    "san francisco"
    "sunnyvale"
    "menlo park"
    "danville / san ramon"
    "walnut creek"})

(defonce -regions-
  (atom regions-blacklist))

(defn p [qstr coll]
  (when-not (empty? coll)
    (printf "** %s\n" qstr)
    (doseq [{:keys [title price region url address] :as e} coll]
      (printf "*** %s\n    Price: $%s\n    Region: %s%s\n    Url: %s\n\n"
              title price region
              (if-not (empty? address) (format "\n   %s" address) "")
              url))))

(defn do-query [visited query]
  (let [all      (craj/query-cl query)
        results  (->> (filter #(>= 2850 (:price %)) all)
                      (remove #(contains? regions-blacklist (:region %)))
                      (distinct-by :preview)
                      (distinct-by :title)
                      (remove #(or (contains? visited (:preview %))
                                   (contains? visited (:title %))
                                   (contains? visited (:url %)))))
        visited' (->> (for [r results
                            k [:preview :title :url]]
                        (get r k))
                      (into visited))]
    (swap! -regions- into (map :region all))
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
                       "hayes valley"
                       "pacific heights"
                       "presidio"
                       "south beach"]
        f             (io/file "/home/arrdem/Dropbox/crawled-apartments.org")
        _             (.createNewFile f)
        g             (io/file "/home/arrdem/Dropbox/visited.edn")
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
