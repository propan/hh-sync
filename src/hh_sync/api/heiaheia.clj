(ns hh-sync.api.heiaheia
  (:require [clj-http.client :as client]
            [net.cgrand.enlive-html :as enlive])
  (:import [java.lang Math]))

(def HH_SPORTS
  {:running           1
   :cycling-transport 2
   :cycling-sport     2
   :mountain-biking   2
   :swimming          13
   :walking           14})

(def HH_DATE_FORMAT "dd.MM.yyyy")

(def HH_LOGIN "https://www.heiaheia.com/login")
(def HH_AUTHENTICATE "https://www.heiaheia.com/account/authenticate")
(def HH_WORKOUTS "https://www.heiaheia.com/training_logs.js")

(defn- parse-duration
  [duration]
  (let [lh (mod duration 3600)
        h  (/ (- duration lh) 3600)
        lm (mod lh 60)
        m  (/ (- lh lm) 60)]
    {:h (int h) :m (int m) :s (java.lang.Math/round lm)}))

(defn- format-date
  [s]
  (-> HH_DATE_FORMAT (java.text.SimpleDateFormat.) (.format s)))

(defn- parse-login-form
  [page]
  (->> (enlive/select (enlive/html-snippet page) [:form :input])
       (map :attrs)
       (map #(vector (:name %) (:value %)))
       (into (hash-map))))

(defn- get-login-form
  [cookie-store]
  (let [response (client/get HH_LOGIN {:cookie-store cookie-store})
        form     (parse-login-form (:body response))]
    (if-let [token (get form "authenticity_token")]
      form
      (throw (Exception. "Could not find HeiaHeia login form.")))))

(defn- authenticate
  [form cookie-store]
  (let [response (client/post HH_AUTHENTICATE {:form-params  form
                                               :cookie-store cookie-store})]
    (= (:status response) 302)))

(defn create-session
  [username password]
  (let [cookie-store (clj-http.cookies/cookie-store)
        login-form   (get-login-form cookie-store)
        success      (authenticate (assoc login-form
                                          "user[email]" username
                                          "user[password]" password)
                                   cookie-store)]
    (if success
      {:token   (get login-form "authenticity_token")
       :cookies cookie-store}
      (throw (Exception. "Incorrect HeiaHeia credentials")))))

(defn create-workout
  [workout session]
  (let [duration (parse-duration (:duration workout))
        response (client/post HH_WORKOUTS {:form-params  {"utf8"                     "✓"
                                                          "authenticity_token"       (:token session)
                                                          "training_log[sport]"      (get HH_SPORTS (:sport workout) 14)
                                                          "training_log[date]"       (format-date (:date workout))
                                                          "training_log[duration_h]" (:h duration)
                                                          "training_log[duration_m]" (:m duration)
                                                          "training_log[duration_s]" (:s duration)
                                                          "training_log[distance]"   (:distance workout)
                                                          "training_log[calories]"   (:calories workout)
                                                          "training_log[avg_hr]"     (:heart-rate-avg workout)
                                                          "training_log[max_hr]"     (:heart-rate-max workout)
                                                          "training_log[favourite]"  false
                                                          "button"                   ""}
                                           :cookie-store (:cookies session)})]
    (= (:status response) 200)))
