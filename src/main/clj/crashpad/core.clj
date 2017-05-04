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
    {:type    ::search
     :query   query
     :visited visited'
     :results results
     :date    (java.util.Date.)}))

(defn do-crawl [visited querries]
  (loop [[q & querries] querries
         visited        visited
         acc            []]
    (if-not (nil? q)
      (let [{:keys [visited] :as r} (do-query visited q)]
        (recur querries visited (conj acc r)))
      {:type    ::crawl
       :date    (java.util.Date.)
       :visited visited
       :results acc})))

(defn ->query [neighborhood]
  {:query   neighborhood
   :area    :sfbay
   :section [:housing :all]
   :params  {"max_price" 2850
             "min_price" 1400
             "hasPic"    1
             ;; "laundry"   [1, 2, 3]
             ;; "nh" [4, 8, 12, 10, 20, 18, 19, 1]
             }})

(defn pr-search [{:keys [results query] :as search}]
  (when-not (empty? results)
    (printf "** %s\n" query)
    (doseq [{:keys [title price region url address] :as e} results]
      (printf "*** %s\n    Price: $%s%s%s\n    Url: %s\n\n"
              title price
              (if region (format "\n    Region: %s" region) "")
              (if-not (empty? address) (format "\n    Address: %s" address) "")
              url))))

(defn pr-crawl [{:keys [results date] :as crawl}]
  (assert (= (:type crawl) ::crawl))
  (when-not (every? empty? (map :results results))
    (printf "* Crawl on %s\n" date)
    (doseq [search results]
      (pr-search @search))))

(defn -main []
  (let [qs      #{"south of market"
                  "soma"
                  "alamo square"
                  "western addition"
                  "hayes valley"
                  "pacific heights"
                  "lower haight"
                  "mission bay"
                  "inner mission"}
        f       (io/file "crawled-apartments.org")
        _       (.createNewFile f)
        g       (io/file "visited.edn")
        _       (if-not (.exists g)
                  (spit g #{}))
        h       (io/file "proxies.txt")
        visited (edn/read-string (slurp g))
        results (binding [*proxies* (atom {:candidates (set (line-seq (io/reader h)))})]
                  (try
                    (do-crawl visited (map ->query qs))
                    (finally
                      ;; Save the updated proxies list
                      (with-open [outf (io/writer h)]
                        (binding [*out* outf]
                          (doseq [c (some->> *proxies* deref :candidates)]
                            (println c))
                          (doseq [c (some->> *proxies* deref :usable)]
                            (println c))))

                      (println "[main] Proxies list dumped"))))]
    
    (with-open [outf (io/writer f :append true)]
      (binding [*out* outf]
        (pr-crawl results)))
    (println "[main] results dumped")

    (with-open [outf (io/writer g)]
      (binding [*out* outf]
        (println (set (:visited results)))))
    (println "[main] visited dumped")))
