(defproject me.arrdem/crashpad "0.0.0"
  :main crashpad.core
  :source-paths ["src/main/clj"]
  :dependencies [[me.arrdem/guten-tag "0.1.6"]
                 [me.arrdem/crajure "0.2.3"]
                 [medley "0.8.1"]
                 [circleci/clj-yaml "0.5.5"]]
  :profiles {:dev  {:source-paths ["src/dev/clj"]}
             :jnt  {:exclusions   [org.clojure/clojure]
                    :dependencies [[org.jaunt-lang/jaunt "0.3.0-SNAPSHOT"]]}
             :clj  {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :user [:base :system :provided :clj]})
