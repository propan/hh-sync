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

(defn- config-file-location
  "Return full path to the configuration file."
  []
  (str (home-directory) "/" CONFIG_FILE))

(defn load-config
  []
  (try
    (with-open [r (io/reader (config-file-location))]
      (read (java.io.PushbackReader. r)))
    (catch Exception e
      nil)))

(defn save-config
  [config]
  (->> (pr config)
       (with-out-str)
       (spit (config-file-location)))
  config)

(defn valid?
  [config]
  (and (-> config :endomondo :username)
       (-> config :endomondo :password)

       (or (-> config :heiaheia :disabled)
           (and
            (-> config :heiaheia :username)
            (-> config :heiaheia :password)))

       (or (-> config :kilometrikisa :disabled)
           (and
            (-> config :kilometrikisa :username)
            (-> config :kilometrikisa :password)
            (-> config :kilometrikisa :contest-id)))))

(defn exists?
  []
  (let [config-file (io/file (config-file-location))]
    (and (.exists config-file)
         (.canRead config-file))))
