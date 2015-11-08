(ns hh-sync.api.endomondo
  (:require [clojure.string :as string]
            [clj-http.client :as client])
  (:import [java.util Locale]
           [java.util UUID]))

(def ENDOMONDO_SPORTS
  {0  :running
   1  :cycling-transport
   2  :cycling-sport
   3  :mountain-biking
   4  :skating
   5  :roller-skiing
   6  :skiing-cross-country
   7  :skiing-downhill
   8  :snowboarding
   9  :kayaking
   10 :kite-surfing
   11 :rowing
   12 :sailing
   13 :windsurfing
   14 :fitness-walking
   15 :golfing
   16 :hiking
   17 :orienteering
   18 :walking
   19 :riding
   20 :swimming
   21 :indoor-cycling
   22 :other
   23 :aerobics
   24 :badminton
   25 :baseball
   26 :basketball
   27 :boxing
   28 :climbing-stairs
   29 :cricket
   30 :elliptical-training
   31 :dancing
   32 :fencing
   33 :american-football
   34 :rugby
   35 :soccer
   36 :handball
   37 :hockey
   38 :pilates
   39 :polo
   40 :scuba-diving
   41 :squash
   42 :table-tennis
   43 :tennis
   44 :beach-volleyball
   45 :indoor-volleyball
   46 :weight-training
   47 :yoga
   48 :martial-arts
   49 :gymnastics
   50 :step-counter
   87 :circuit-training
   88 :treadmill-running
   89 :skateboarding
   90 :surfing
   91 :snow-shoveling
   92 :wheelchair
   93 :climbing
   94 :treadmill-walking})

(def ENDOMONDO_DATE_FORMAT "yyyy-MM-dd HH:mm:ss z")

(def E_AUTHENTICATE "https://api.mobile.endomondo.com/mobile/auth")
(def E_WORKOUTS "https://api.mobile.endomondo.com/mobile/api/workouts")
(def E_ACCOUNT "https://api.mobile.endomondo.com/mobile/api/profile/account/get")

(defn- create-device-info
  []
  {:os         ""
   :model      ""
   :osVersion  ""
   :vendor     "propan/hh-sync"
   :appVariant "0.0.1"
   :country    "FI"
   :v          "2.4"
   :appVersion "0.0.1"
   :deviceId   (.toString (UUID/randomUUID))})

(defn- parse-tokens
  [body]
  (->>
   (rest (clojure.string/split body #"\n"))
   (map #(clojure.string/split % #"="))
   (filter #(or (= "authToken" (first %)) (= "secureToken" (first %))))
   (into (hash-map))))

(defn- parse-date
  [s]
  (-> ENDOMONDO_DATE_FORMAT (java.text.SimpleDateFormat.) (.parse s)))

(defn- workout-data
  [raw-data]
  {:id             (:id raw-data)
   :sport          (get ENDOMONDO_SPORTS (:sport raw-data) :unknown)
   :calories       (:calories raw-data)
   :heart-rate-avg (:heart_rate_avg raw-data)
   :heart-rate-max (:heart_rate_max raw-data)
   :date           (parse-date (:start_time raw-data))
   :duration       (:duration raw-data)
   :distance       (:distance raw-data)})

(defn create-session
  [{:keys [username password]}]
  (let [cookie-store (clj-http.cookies/cookie-store)
        device-info  (create-device-info)
        query-params (merge device-info
                            {:action   "pair"
                             :email    username
                             :password password})
        response     (client/get E_AUTHENTICATE
                                 {:query-params query-params
                                  :cookie-store cookie-store})]
    (if-let [token (and (= (:status response) 200)
                        (-> response
                            (:body)
                            (parse-tokens)
                            (get "authToken")))]
      {:token   token
       :cookies cookie-store}
      (throw (Exception. "Incorrect Endomondo credentials.")))))

(defn valid-credentials?
  [credentials]
  (try
    (create-session credentials)
    true
    (catch Exception e
      false)))

(defn get-workouts
  [session & {:keys [maxResults before] :or {maxResults 20}}]
  (let [response (client/get E_WORKOUTS {:query-params {:authToken  (:token session)
                                                        :language   "en"
                                                        :maxResults maxResults
                                                        :fields     (clojure.string/join "," ["device" "simple" "basic" "lcp_count"])}
                                         :cookie-store (:cookies session)
                                         :as           :json})]
    (if (= (:status response) 200)
      (map workout-data (-> response :body :data))
      (throw (Exception. "Failed to fetch workouts from Endomondo.")))))

(defn get-account
  [session]
  (let [response (client/get E_ACCOUNT {:query-params {:authToken  (:token session)
                                                       :language   "en"
                                                       :fields     (clojure.string/join "," ["hr_zones" "emails"])}
                                        :cookie-store (:cookies session)
                                        :as           :json})]
    (if (= (:status response) 200)
      (-> response :body :data)
      (throw (Exception. "Failed to fetch account information from Endomondo.")))))
