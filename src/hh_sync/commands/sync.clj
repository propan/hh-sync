(ns hh-sync.commands.sync
  (:require [hh-sync.config :as config]
            [hh-sync.api.heiaheia :as heiaheia]
            [hh-sync.api.endomondo :as endomondo]
            [hh-sync.cli-utils :refer [exit]]))

(defn perform-sync
  [settings]
  (when-not (config/valid? settings)
    (exit 1 "Configuration file is corrupted. Please run hh-sync --configure."))

  (exit 0 "Syncing has completed."))

(defn sync-workouts
  [interactive]
  (when-not (config/exists?)
    (exit 1 "Could not find configuration file. Please run hh-sync --configure."))

  (if-let [settings (config/load-config)]
    (perform-sync settings)
    (exit 1 "Configuration file is corrupted. Please run hh-sync --configure.")))

