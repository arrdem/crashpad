(defproject arrdem/crashpad "0.0.0"
  :source-paths ["src/main/clj"]
  :dependencies [[me.arrdem/guten-tag "0.1.6"]
                 [crajure "0.1.1"]]
  :profiles {:dev  {:source-paths ["src/dev/clj"]}
             :jnt  {:exclusions   [org.clojure/clojure]
                    :dependencies [[org.jaunt-lang/jaunt "0.3.0-SNAPSHOT"]]}
             :clj  {:dependencies [[org.clojure/clojure "1.9.0-master-SNAPSHOT"]]}
             :user [:base :system :provided :clj]})
