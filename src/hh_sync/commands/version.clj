(ns hh-sync.commands.version
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [hh-sync.cli-utils :refer [exit]]))

(defn- hh-sync-version
  []
  (or (System/getenv "HH_SYNC_VERSION")
      (with-open [reader (-> "META-INF/maven/hh-sync/hh-sync/pom.properties"
                             io/resource
                             io/reader)]
        (-> (doto (java.util.Properties.)
              (.load reader))
            (.getProperty "version")))))

(defn version
  "Print version for hh-sync and the current JVM."
  []
  (exit 0 (s/join " " ["hh-sync" (hh-sync-version)
                       "on Java" (System/getProperty "java.version")
                       (System/getProperty "java.vm.name")])))
