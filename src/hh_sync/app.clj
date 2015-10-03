(ns hh-sync.app
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [hh-sync.api.heiaheia :as heiaheia]
            [hh-sync.api.endomondo :as endomondo])
  (:gen-class :main true))

(def CONFIG_FILE ".hh-sync")

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
        (println question (str "(" (string/join "/" options) ")"))
        (recur (read-line))))))

(defn prompt-string
  "Prompts a user with the given question and returns the given answer."
  [question]
  (loop [answer nil]
    (if-not (string/blank? answer)
      answer
      (do
        (println question)
        (recur (read-line))))))

;;
;; Configuration
;;

(defn config-exists?
  []
  (let [config-file (io/file CONFIG_FILE)]
    (and (.exists config-file)
         (.canRead config-file))))

(defn prompt-credentials
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
  (when (config-exists?)
    (when (= "n" (prompt "Looks like you already have a config file. Would you like to overwrite it?" #{"y" "n"}))
      (exit 0 "Canceled by user.")))

  (let [endomondo-cred (prompt-credentials "Endomondo" endomondo/valid-credentials?)
        heiaheia-cred  (prompt-credentials "HeiaHeia" heiaheia/valid-credentials?)]
    (spit CONFIG_FILE (with-out-str (pr {:endomondo endomondo-cred
                                         :heiaheia  heiaheia-cred
                                         :last-sync nil})))))

(defn load-config
  []
  (try
    (with-open [r (io/reader CONFIG_FILE)]
      (read (java.io.PushbackReader. r)))
    (catch Exception e
      nil)))

;;
;; Syncing
;;

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

