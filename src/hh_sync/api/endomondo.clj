(ns hh-sync.api.endomondo
  (:require [clojure.string :as string]
            [clj-http.client :as client])
  (:import [java.util Locale]
           [java.util UUID]))

(def ENDOMONDO_SPORTS
  {0 :running
   1 :cycling-transport
   2 :cycling-sport
   3 :mountain-biking})

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
  [username password]
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
