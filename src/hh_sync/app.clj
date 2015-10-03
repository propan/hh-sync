(ns hh-sync.app
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :refer [reader]]
            [clojure.string :as string]
            [hh-sync.api.heiaheia :as heiaheia]
            [hh-sync.api.endomondo :as endomondo])
  (:gen-class :main true))

(def cli-options
  [["-c" "--configure" "Configure hh-sync"]
   ["-s" "--sync" "Run syncronization"]
   ["-h" "--help"]])

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn usage
  [options-summary]
  (->> ["Options:"
        options-summary
        ""]
       (string/join \newline)))

(defn prompt
  "Prompts a user with the given question and a set of acceptable answers."
  [question options]
  (loop [a nil]
    (if (contains? options a)
      a
      (do
        (println question)
        (recur (read-line))))))

(defn configure
  []
  (println "Hello, friend!"))

(defn load-config
  []
  (try
    (with-open [r (reader ".hh-sync")]
      (read (java.io.PushbackReader. r)))
    (catch Exception e
      nil)))

(defn sync-workouts
  []
  (if-let [config (load-config)]
    (println "sync")
    (println "Could not find configuration file. Please run hh-sync --configure.")))

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

