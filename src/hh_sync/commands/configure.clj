(ns hh-sync.commands.configure
  (:require [hh-sync.api.heiaheia :as heiaheia]
            [hh-sync.api.endomondo :as endomondo]
            [hh-sync.api.kilometrikisa :as kilometrikisa]
            [hh-sync.config :as config]
            [hh-sync.cli-utils :refer [exit prompt prompt-string]]))

(defn- prompt-credentials
  [name validator? options]
  (loop []
    (let [username    (prompt-string (str "What is your " name " username?"))
          password    (prompt-string (str "What is your " name " password?"))
          credentials (assoc options :username username :password password)]
      (if (validator? credentials)
        credentials
        (do
          (println "Incorrect username or password.")
          (when (= "n" (prompt "Do you want to try again?" #{"y" "n"}))
            (exit 0 "Canceled by user."))
          (recur))))))

(defn- prompt-heiaheia-login-type
  []
  (let [answer (prompt "Would you like to login to HeiaHeia using your Facebook account?" #{"y" "n"})]
    (if (= answer "y")
      :facebook
      :heiaheia)))

(defn- configure-heiaheia-sync
  []
  (let [answer (prompt "Would you like to sync your workouts to HeiaHeia?" #{"y" "n"})]
    (if (= answer "y")
      (let [heiaheia-login   (prompt-heiaheia-login-type)
            heiaheia-account (if (= heiaheia-login :facebook) "Facebook" "HeiaHeia")]
        (prompt-credentials heiaheia-account heiaheia/valid-credentials? {:type        heiaheia-login
                                                                          :disabled    false
                                                                          :last-synced nil}))
      {:disabled true})))

;; TODO: prompt kilometrikisa contest id and check that it exists

(defn- configure-kilometrikisa-sync
  []
  (let [answer (prompt "Would you like to sync your workouts to Kilometrikisa?" #{"y" "n"})]
    (if (= answer "y")
      (prompt-credentials "Kilometrikisa" kilometrikisa/valid-credentials? {:disabled    false
                                                                            :last-synced nil})
      {:disabled true})))

(defn configure
  []
  (when (config/exists?)
    (when (= "n" (prompt "Looks like you already have a config file. Would you like to overwrite it?" #{"y" "n"}))
      (exit 0 "Canceled by user.")))

  (println "You are just one step away from automatic syncing of your workouts from Endomondo to HeiaHeia!\n")

  (let [endomondo-cred       (prompt-credentials "Endomondo" endomondo/valid-credentials? {})
        heiaheia-config      (configure-heiaheia-sync)
        kilometrikisa-config (configure-kilometrikisa-sync) ]
    (config/save-config {:endomondo     endomondo-cred
                         :heiaheia      heiaheia-config
                         :kilometrikisa kilometrikisa-config})
    (exit 0 "Configuration has completed. Run hh-sync --sync")))
