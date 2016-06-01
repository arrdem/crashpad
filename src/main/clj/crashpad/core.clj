(ns crashpad.core
  (:require [crajure.core :as craj]
            [crajure.util :refer [*proxies*]]
            [medley.core :refer :all]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def regions-blacklist
  #{"sonoma"
    "redwood city"
    "portola valley"
    "mill valley"
    "palo alto"
    "morgan hill"
    "oakland lake merritt / grand"
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
      (printf "*** %s\n    Price: $%s%s%s\n    Url: %s\n\n"
              title price
              (if region (format "\n    Region: %s" region) "")
              (if-not (empty? address) (format "\n    Address: %s" address) "")
              url))))

(defn do-query [visited query]
  (let [all      (craj/query-cl query)
        ;; FIXME: annotate results with rent control status if possible
        ;; - http://propertymap.sfplanning.org/
        ;; -- Backing service has year built information
        ;; -- Lot certificate of occupancy prior to June 13, 1979
        ;; -- YRBUILT in layer 14, "Assessor" is the target field
        results  (->> all
                      shuffle
                      ;; List({:title, :url, :price})
                      (remove #(contains? regions-blacklist (:region %)))
                      ;; List(...) such that regions are desirable
                      (distinct-by :title)
                      ;; List(...) such that ... & titles are unique
                      #_(map craj/item-map->preview+address)
                      ;; List({:title, :url, :address, :price, :preview, :address}) & ...
                      #_(distinct-by :preview)
                      ;; List(...) such that ... & previews are unique
                      #_(distinct-by #(get % :address (Object.)))
                      ;; List(...) such that ... & addresses if present are unique
                      (remove #(or #_(contains? visited (:preview %))
                                   #_(contains? visited (:title %))
                                   (contains? visited (:url %))
                                   #_(contains? visited (:address %)))))
        visited' (->> (for [r     results
                            k     [#_:preview :title :url #_:address]
                            :let  [r (get r k)]
                            :when r]
                        r)
                      (into visited))]
    (swap! -regions- into (map :region all))
    {:visited visited'
     :query   query
     :results results}))

(defn ->query [neighborhood]
  {:query   neighborhood
   :area    "sfbay"
   :section [:housing :apartments]
   :params  {"max_price" 2850
             "min_price" 1400}})

(defn -main []
  (let [neighborhoods ["south of market"
                       "soma"
                       "alamo square"
                       "western addition"
                       "hayes valley"
                       "pacific heights"
                       "presidio"
                       "south beach"]
        f             (io/file "crawled-apartments.org")
        _             (.createNewFile f)
        g             (io/file "visited.edn")
        _             (if-not (.exists g)
                        (spit g #{}))
        h             (io/file "proxies.txt")
        visited       (edn/read-string (slurp g))]
    (binding [*proxies* (vec (line-seq (io/reader h)))]
      (with-open [outf (io/writer f :append true)]
        (binding [*out* outf]
          (printf "* Crawl on %s\n" (java.util.Date.)))
        (loop [visited             visited
               [n & neighborhoods] neighborhoods
               acc                 0]
          (if n
            (do (println "Searching in" n)
                (let [{:keys [visited results]} (do-query visited (->query n))]
                  ;; update output file
                  (binding [*out* outf]
                    (p n results))

                  (recur visited neighborhoods (+ acc (count results)))))
            (do
              ;; generate visited file
              (spit (io/writer g) visited)

              (println "Done! found" acc "places :D"))))))))
