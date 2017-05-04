(ns crashpad.core
  (:require [crajure.core :as craj]
            [crajure.util :refer [*proxies*]]
            [medley.core :refer :all]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(defn do-query [visited query]
  (let [;; FIXME: annotate results with rent control status if possible
        ;; - http://propertymap.sfplanning.org/
        ;; -- Backing service has year built information
        ;; -- Lot certificate of occupancy prior to June 13, 1979
        ;; -- YRBUILT in layer 14, "Assessor" is the target field

        results  (->> (craj/query-cl query)
                      (take-while #(not (contains? visited (:url %)))))
        visited' (->> (for [r     results
                            k     [#_:preview #_:title :url #_:address]
                            :let  [r (get r k)]
                            :when r]
                        r)
                      (into visited))]
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
   :section [:housing :apartments]
   :params  {"max_price" 2850
             "min_price" 1400
             "hasPic"    1
             "sort"      "date"
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
  (printf "* Crawl on %s\n" date)
  (doseq [search results]
    (pr-search search)))

(defn pr-proxies [{:keys [candidates usable]}]
  (doseq [proxy (concat candidates usable)
          :when proxy]
    (println proxy)))

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
                      (when *proxies*
                        (with-open [outf (io/writer h)]
                          (binding [*out* outf]
                            (pr-proxies @*proxies*)))
                        (println "[main] Proxies list dumped")))))
        
        {:keys [results visited]} crawl
        result-count              (apply + (map #(-> % :results count) results))]

    (when-not (zero? result-count)
      (printf "[main] found %d new listings" result-count)
      (with-open [outf (io/writer f :append true)]
        (binding [*out* outf]
          (pr-crawl crawl)))
      (println "[main] results dumped")

      (with-open [outf (io/writer g)]
        (binding [*out* outf]
          (prn visited)))
      (println "[main] visited dumped"))))
