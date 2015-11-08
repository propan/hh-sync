(ns hh-sync.commands.configure
  (:require [hh-sync.api.heiaheia :as heiaheia]
            [hh-sync.api.endomondo :as endomondo]
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

(defn configure
  []
  (when (config/exists?)
    (when (= "n" (prompt "Looks like you already have a config file. Would you like to overwrite it?" #{"y" "n"}))
      (exit 0 "Canceled by user.")))

  (println "You are just one step away from automatic syncing of your workouts from Endomondo to HeiaHeia!\n")

  (let [endomondo-cred   (prompt-credentials "Endomondo" endomondo/valid-credentials? {})
        heiaheia-login   (prompt-heiaheia-login-type)
        heiaheia-account (if (= heiaheia-login :facebook) "Facebook" "HeiaHeia")
        heiaheia-cred    (prompt-credentials heiaheia-account heiaheia/valid-credentials? {:type heiaheia-login})]
    (config/save-config {:endomondo   endomondo-cred
                         :heiaheia    heiaheia-cred
                         :last-synced nil})
    (exit 0 "Configuration has completed. Run hh-sync --sync")))
