(ns hh-sync.config
  (:require [clojure.java.io :as io]))

(def CONFIG_FILE "hh-sync.conf")

(defn- getenv
  "Wrap System/getenv for testing purposes."
  [name]
  (System/getenv name))

(defn- home-directory
  "Return full path to the user's hh-sync home directory."
  []
  (let [hh-sync-home (getenv "HH_SYNC_HOME")
        hh-sync-home (or (and hh-sync-home (io/file hh-sync-home))
                         (io/file (System/getProperty "user.home") ".hh-sync"))]
    (.getAbsolutePath (doto hh-sync-home .mkdirs))))

(defn load-config
  []
  (try
    (with-open [r (io/reader (str (home-directory) "/" CONFIG_FILE))]
      (read (java.io.PushbackReader. r)))
    (catch Exception e
      nil)))

(defn save-config
  [config]
  (->> (pr config)
       (with-out-str)
       (spit CONFIG_FILE))
  config)

(defn valid?
  [config]
  (and (-> config :endomondo :username)
       (-> config :endomondo :password)
       (-> config :heiaheia :username)
       (-> config :heiaheia :password)))

(defn exists?
  []
  (let [config-file (io/file (str (home-directory) "/" CONFIG_FILE))]
    (and (.exists config-file)
         (.canRead config-file))))
