(ns hh-sync.app
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [hh-sync.cli-utils :refer [exit]]
            [hh-sync.commands.configure :refer [configure]]
            [hh-sync.commands.sync :refer [sync-workouts]])
  (:gen-class :main true))

(def cli-options
  [["-c" "--configure" "Configure hh-sync"]
   ["-s" "--sync" "Run syncronization"]
   ["-h" "--help"]])

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn usage
  [options-summary]
  (->> ["Options:"
        options-summary
        ""]
       (string/join \newline)))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)          (exit 0 (usage summary))
      (not= (count options) 1) (exit 1 (usage summary))
      errors                   (exit 1 (error-msg errors)))

    (when (:configure options)
      (configure))
    
    (when (:sync options)
      (sync-workouts))))

