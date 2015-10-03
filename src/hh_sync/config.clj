(ns hh-sync.config
  (:require [clojure.java.io :as io]))

(def CONFIG_FILE ".hh-sync")

(defn load-config
  []
  (try
    (with-open [r (io/reader CONFIG_FILE)]
      (read (java.io.PushbackReader. r)))
    (catch Exception e
      nil)))

(defn save-config
  [config]
  (spit CONFIG_FILE (with-out-str (pr config))))

(defn valid?
  [config]
  (and (-> config :endomondo :username)
       (-> config :endomondo :password)
       (-> config :heiaheia :username)
       (-> config :heiaheia :password)))

(defn exists?
  []
  (let [config-file (io/file CONFIG_FILE)]
    (and (.exists config-file)
         (.canRead config-file))))
