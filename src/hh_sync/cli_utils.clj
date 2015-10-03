(ns hh-sync.cli-utils
  (:require [clojure.string :as string]))

(defn exit [status msg]
  (println msg)
  (System/exit status))

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

