(ns hh-sync.commands.sync
  (:require [hh-sync.config :as config]
            [hh-sync.api.heiaheia :as heiaheia]
            [hh-sync.api.endomondo :as endomondo]
            [hh-sync.cli-utils :refer [exit prompt]]))

(defn- create-session
  [construct-fn options]
  (try
    (construct-fn options)
    (catch Exception e
      (exit 1 (str (.getMessage e) "\n Please run hh-sync --configure again.")))))

(defn- find-workouts-to-sync
  [workouts last-synced]
  ;; is it really a way?
  (->> workouts
       (sort-by :date)
       (reverse)
       (take-while #(not= (:id %) last-synced))
       (reverse)))

(defn- set-synced!
  [settings id]
  (-> settings
      (assoc :last-synced id)
      (config/save-config)))

(defn- workout-date
  [date]
  (-> "dd.MM.yyyy HH:mm:ss" (java.text.SimpleDateFormat.) (.format date)))

(defn- workout-repr
  [workout]
  (str (name (:sport workout)) " " (:id workout) " at " (workout-date (:date workout))))

(defn- create-heiaheia-workout!
  [session workout settings]
  (heiaheia/create-workout session workout)
  (println "Synced:" (workout-repr workout))
  (set-synced! settings (:id workout)))

(defn- perform-sync
  [settings interactive]
  (when-not (config/valid? settings)
    (exit 1 "Configuration file is corrupted. Please run hh-sync --configure."))

  (let [es (create-session endomondo/create-session (:endomondo settings))
        hs (create-session heiaheia/create-session (:heiaheia settings))]
    (try
      (let [workouts (endomondo/get-workouts es)
            workouts (find-workouts-to-sync workouts (:last-synced settings))]
        (when-not (seq workouts)
          (exit 0 "Everything is in sync."))
        (reduce (fn [settings workout]
                  (if interactive
                    (let [answer (prompt (str "Would you like to sync " (workout-repr workout) "?") #{"y" "n" "a"})]
                      (case answer
                        "a" (exit 0 "Interrupted by user.")
                        "y" (create-heiaheia-workout! hs workout settings)
                        "n" (set-synced! settings (:id workout))))
                    (create-heiaheia-workout! hs workout settings)))
                settings
                workouts))
      (catch Exception e
        (exit 1 (or (.getMessage e)
                    "Somthing terrifying has happend. I don't know what it is.")))))

  (exit 0 "Syncing has completed."))

(defn sync-workouts
  [interactive]
  (when-not (config/exists?)
    (exit 1 "Could not find configuration file. Please run hh-sync --configure."))

  (if-let [settings (config/load-config)]
    (perform-sync settings interactive)
    (exit 1 "Configuration file is corrupted. Please run hh-sync --configure.")))

