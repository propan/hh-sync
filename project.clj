(defproject hh-sync "0.0.1-SNAPSHOT"
  :description "a command-line utility for syncing workouts from Endomondo to HeiaHeia"
  :url "https://github.com/propan/hh-sync"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-http "2.0.0"]
                 [cheshire "5.5.0"]
                 [enlive "1.1.6"]
                 [org.clojure/tools.cli "0.3.3"]]
  :main ^:skip-aot hh-sync.app
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
