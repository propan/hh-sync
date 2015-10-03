(ns hh-sync.commands.configure
  (:require [hh-sync.api.heiaheia :as heiaheia]
            [hh-sync.api.endomondo :as endomondo]
            [hh-sync.config :as config]
            [hh-sync.cli-utils :refer [exit prompt prompt-string]]))

(defn- prompt-credentials
  [name validator?]
  (loop []
    (let [username (prompt-string (str "What is your " name " username?"))
          password (prompt-string (str "What is your " name " password?"))]
      (if (validator? username password)
        {:username username :password password}
        (do
          (println "Incorrect username or password.")
          (when (= "n" (prompt "Do you want to try again?" #{"y" "n"}))
            (exit 0 "Canceled by user."))
          (recur))))))

(defn configure
  []
  (when (config/exists?)
    (when (= "n" (prompt "Looks like you already have a config file. Would you like to overwrite it?" #{"y" "n"}))
      (exit 0 "Canceled by user.")))

  (let [endomondo-cred (prompt-credentials "Endomondo" endomondo/valid-credentials?)
        heiaheia-cred  (prompt-credentials "HeiaHeia" heiaheia/valid-credentials?)]
    (config/save-config {:endomondo   endomondo-cred
                         :heiaheia    heiaheia-cred
                         :last-synced nil})
    (exit 0 "Configuration has completed. Run hh-sync --sync")))
