(ns hh-sync.app
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [hh-sync.cli-utils :refer [exit]]
            [hh-sync.commands.configure :refer [configure]]
            [hh-sync.commands.sync :refer [sync-workouts]]
            [hh-sync.commands.version :refer [version]])
  (:gen-class :main true))

(def cli-options
  [["-c" "--configure" "Configure hh-sync"]
   ["-s" "--sync" "Sync workouts to HeiaHeia"]
   ["-i" "--interactive" "You will be prompt about need of syncing of every workout fetched from Endomondo"
    :default false]
   ["-d" "--depth DEPTH" "the depth of new workout lookup"
    :default 20
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 20 % 151) "the depth must be a number between 20 and 150"]]
   ["-v" "--version" "prints hh-sync version"]
   ["-h" "--help"]])

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn usage
  [options-summary]
  (->> ["hh-sync - a command-line utility for syncing workouts from Endomondo to HeiaHeia"
        ""
        "Example: hh-sync --sync --interactive"
        ""
        "Options:"
        options-summary
        ""]
       (string/join \newline)))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (when errors
      (exit 1 (error-msg errors)))

    (when (:help options)
      (exit 0 (usage summary)))

    (when (:version options)
      (version))

    (when (:configure options)
      (configure))
    
    (when (:sync options)
      (sync-workouts (:interactive options) (:depth options)))

    (exit 0 (usage summary))))

