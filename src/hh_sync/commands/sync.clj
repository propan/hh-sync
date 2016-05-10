(ns hh-sync.commands.sync
  (:require [hh-sync.config :as config]
            [hh-sync.api.heiaheia :as heiaheia]
            [hh-sync.api.endomondo :as endomondo]
            [hh-sync.api.kilometrikisa :as kilometrikisa]
            [hh-sync.cli-utils :refer [exit prompt]]))

(defn- create-session
  [name construct-fn options]
  (try
    (construct-fn options)
    (catch Exception e
      (exit 1 (str "Could not create " name " session due to: " (.getMessage e) "\nYour credentials might be incorrect. Try to run hh-sync --configure again.")))))

(defn- find-workouts-to-sync
  [workouts last-synced]
  ;; is it really a way?
  (->> workouts
       (sort-by :date)
       (reverse)
       (take-while #(not= (:id %) last-synced))
       (reverse)))

(defn- set-synced!
  [settings source id]
  (-> settings
      (assoc-in [source :last-synced] id)
      (config/save-config)))

(defn- workout-date
  [date]
  (-> "dd.MM.yyyy HH:mm:ss" (java.text.SimpleDateFormat.) (.format date)))

(defn- workout-repr
  [workout]
  (str (name (:sport workout)) " " (:id workout) " at " (workout-date (:date workout))))

(defn- add-days
  [date days]
  (.getTime
   (doto (java.util.Calendar/getInstance)
     (.setTime date)
     (.add java.util.Calendar/DAY_OF_MONTH days))))

;;
;; HeiaHeia Sync
;;

(defn- create-heiaheia-workout!
  [session settings workout]
  (heiaheia/create-workout session workout)
  (println "Synced:" (workout-repr workout))
  (set-synced! settings :heiaheia (:id workout)))

(defn- perform-heiaheia-sync
  [settings workouts interactive]
  (if (-> settings :heiaheia :disabled)
    settings
    (let [hs       (create-session "HeiaHeia" heiaheia/create-session (:heiaheia settings))
          workouts (find-workouts-to-sync workouts (-> settings :heiaheia :last-synced))]
      (if-not (seq workouts)
        ;; no need to sync anything
        (do
          (println "HeiaHeia is already in sync.")
          settings)
        ;; let's sync
        (reduce (fn [settings workout]
                  (if interactive
                    (let [answer (prompt (str "Would you like to sync " (workout-repr workout) " to HeiaHeia?") #{"y" "n" "a"})]
                      (case answer
                        "a" (exit 0 "Interrupted by user.")
                        "y" (create-heiaheia-workout! hs settings workout)
                        "n" (set-synced! settings :heiaheia (:id workout))))
                    (create-heiaheia-workout! hs settings workout)))
                settings
                workouts)))))

;;
;; Kilometrikisa Sync
;;

(defn- filter-out-kilometrikisa-workouts
  [workouts]
  (filter (fn [{:keys [sport]}]
            (or (= sport :cycling-transport)
                (= sport :cycling-sport)
                (= sport :mountain-biking))) workouts))

(defn- prepare-kilometrikisa-stats
  [workouts]
  {:start (-> workouts first :date)
   :end   (-> workouts last :date)
   :stats (reduce (fn [stats workout]
                    (let [workout-date (kilometrikisa/format-date (:date workout))
                          distance     (get stats workout-date 0.0)]
                      (assoc stats workout-date (+ distance (:distance workout)))))
                  {}
                  workouts)})

(defn- merge-kilometrikisa-stats
  [updated current]
  (reduce (fn [stats [date distance]]
            (let [current-distance (get current date 0.0)]
              (assoc stats date (+ current-distance distance)))) {} updated))

(defn- update-kilometrikisa-stats!
  [session settings {:keys [last-synced workouts]}]
  (let [stats (prepare-kilometrikisa-stats workouts)
        logs  (kilometrikisa/get-logs session (add-days (:start stats) -1) (add-days (:end stats) 1))
        stats (merge-kilometrikisa-stats (:stats stats) logs)]
    (doseq [[date amount] stats]
      (if-not (kilometrikisa/save-day-stats session date amount)
        (exit 1 (str "Failed to update Kilometrikisa stats for: " date))
        (println "Synced:" date)))
    (set-synced! settings :kilometrikisa last-synced)))

(defn- perform-kilometrikisa-sync
  [settings workouts interactive]
  (if (-> settings :kilometrikisa :disabled)
    settings
    (let [ks       (create-session "Kilometrikisa" kilometrikisa/create-session (:kilometrikisa settings))
          workouts (find-workouts-to-sync workouts (-> settings :kilometrikisa :last-synced))
          workouts (filter-out-kilometrikisa-workouts workouts)]
      (if-not (seq workouts)
        ;; no need to sync anything
        (do
          (println "Kilometrikisa is already in sync.")
          settings)
        ;; let's sync
        (update-kilometrikisa-stats! ks settings
                                     (if interactive
                                       (reduce (fn [context workout]
                                                 (let [answer (prompt (str "Would you like to sync " (workout-repr workout) " to Kilometrikisa?") #{"y" "n" "a"})]
                                                   (case answer
                                                     "a" (exit 0 "Interrupted by user.")
                                                     "y" {:last-synced (:id workout)
                                                          :workouts    (conj (:workouts context) workout)}
                                                     "n" (assoc context :last-synced (:id workout)))))
                                               {:last-synced nil :workouts []} workouts)
                                       {:last-synced (-> workouts last :id)
                                        :workouts    workouts}))))))

;;
;; Main functions
;;

(defn- perform-sync
  [settings interactive depth]
  (when-not (config/valid? settings)
    (exit 1 "Configuration file is corrupted. Please run hh-sync --configure."))

  (try
    (let [es       (create-session "Endomondo" endomondo/create-session (:endomondo settings))
          workouts (endomondo/get-workouts es :max-results depth)]
      (-> settings
          (perform-heiaheia-sync workouts interactive)
          (perform-kilometrikisa-sync workouts interactive))
      (exit 0 "Syncing has completed."))
    (catch Exception e
      (exit 1 (or (.getMessage e)
                  "Something terrifying has happend. I don't know what it is.")))))

(defn sync-workouts
  [interactive depth]
  (when-not (config/exists?)
    (exit 1 "Could not find configuration file. Please run hh-sync --configure."))

  (if-let [settings (config/load-config)]
    (perform-sync settings interactive depth)
    (exit 1 "Configuration file is corrupted. Please run hh-sync --configure.")))

