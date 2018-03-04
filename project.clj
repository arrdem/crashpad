(defproject me.arrdem/crashpad "0.0.0"
  :description "A quick and dirty craigslist search and de-dupe tool."
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [me.arrdem/crajure "0.4.1"]
                 [medley "0.8.1"]]

  :source-paths      ["src/main/clj"
                      "src/main/cljc"]
  :java-source-paths ["src/main/jvm"]
  :test-paths        ["src/test/clj"
                      "src/test/cljc"]
  :resource-paths ["src/main/resources"]

  :profiles {:dev {:dependencies      []
                   :source-paths      ["src/dev/clj"
                                       "src/dev/clj"]
                   :java-source-paths ["src/dev/jvm"]
                   :resource-paths    ["src/dev/resources"]}}

  :main crashpad.core)
