(ns hh-sync.api.heiaheia
  (:require [clj-http.client :as client]
            [net.cgrand.enlive-html :as enlive])
  (:import [java.lang Math]))

(def HH_SPORTS
  {:running              1
   :skiing-downhill      33
   :swimming             13
   :boxing               26
   :cycling-transport    2
   :badminton            15
   :polo                 20497
   :treadmill-running    1621
   :weight-training      247
   :skating              29
   :wheelchair           292
   :golfing              9
   :martial-arts         27
   :step-counter         21748
   :indoor-cycling       36
   :dancing              17
   :fencing              52
   :scuba-diving         34
   :snow-shoveling       68
   :american-football    99
   :windsurfing          59
   :other                14
   :surfing              38
   :handball             90
   :squash               39
   :skateboarding        194
   :tennis               31
   :cricket              61
   :beach-volleyball     93
   :climbing             21
   :skiing-cross-country 6
   :climbing-stairs      42
   :baseball             86
   :rugby                50
   :orienteering         57
   :mountain-biking      49
   :sailing              72
   :cycling-sport        2
   :aerobics             16
   :yoga                 20
   :soccer               22
   :riding               45
   :rowing               40
   :kayaking             111
   :roller-skiing        102
   :fitness-walking      4
   :indoor-volleyball    44
   :basketball           24
   :hiking               79
   :pilates              46
   :circuit-training     35
   :elliptical-training  48
   :kite-surfing         69
   :walking              14
   :table-tennis         37
   :hockey               25
   :treadmill-walking    1621
   :snowboarding         55
   :gymnastics           53})

(def HH_DATE_FORMAT "dd.MM.yyyy")

(def HH_LOGIN "https://www.heiaheia.com/login")
(def HH_AUTHENTICATE "https://www.heiaheia.com/account/authenticate")
(def HH_WORKOUTS "https://www.heiaheia.com/training_logs.js")

(def HH_FACEBOOK_LOGIN "https://www.heiaheia.com/facebook_oauth/start")

(def USER_AGENT "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36")
(def FACEBOOK_URL "https://www.facebook.com")

(defn get-or
  [xs k d]
  (if-let [v (get xs k)] v d))

(defn- parse-duration
  [duration]
  (let [lh (mod duration 3600)
        h  (/ (- duration lh) 3600)
        lm (mod lh 60)
        m  (/ (- lh lm) 60)]
    {:h (int h) :m (int m) :s (java.lang.Math/round (* 1.0 lm))}))

(defn- format-date
  [date]
  (-> HH_DATE_FORMAT (java.text.SimpleDateFormat.) (.format date)))

(defn- parse-login-form
  [page]
  (->> (enlive/select page [:form :input])
       (map :attrs)
       (map #(vector (:name %) (:value %)))
       (into (hash-map))))

(defn- parse-form-action
  [page]
  (->> (enlive/select page [:form])
       (first)
       (:attrs)
       (:action)))

(defn- find-authenticity-token
  [response]
  (as-> (enlive/select (enlive/html-snippet response) [:meta]) elements
    (filter (fn [tag] (= (-> tag :attrs :name) "csrf-token")) elements)
    (first elements)
    (get-in elements [:attrs :content])))

;;
;; Login via Facebook
;;

(defn- submit-facebook-login-form
  [form action cookie-store]
  (let [response (client/post (str FACEBOOK_URL action)
                              {:form-params     form
                               :headers         {"origin"     FACEBOOK_URL
                                                 "referer"    action
                                                 "user-agent" USER_AGENT}
                               :cookies         {"noscript" {:path "/", :value "1"}}
                               :cookie-store    cookie-store})]
    (when-not (= (:status response) 302)
      (throw (Exception. "Facebook authentication failed.")))
    ;; follow redirect
    (client/get (get-in response [:headers "Location"])
                {:headers      {"user-agent" USER_AGENT}
                 :cookie-store cookie-store})))

(defn- create-session-via-facebook
  [username password]
  (let [cookie-store (clj-http.cookies/cookie-store)
        response     (client/get HH_FACEBOOK_LOGIN {:cookie-store cookie-store})
        page         (enlive/html-snippet (:body response))
        action       (parse-form-action page)
        form         (parse-login-form page)
        response     (submit-facebook-login-form (assoc form
                                                        "default_persistent" "0"
                                                        "email" username
                                                        "pass" password)
                                                 action
                                                 cookie-store)]
    (if-let [token (find-authenticity-token (:body response))]
      {:token   token
       :cookies cookie-store}
      (throw (Exception. "Could login to HeiaHeia via Facebook.")))))

;;
;; Login via HeiaHeia form
;;

(defn- submit-heiaheia-login-form
  [form cookie-store]
  (client/post HH_AUTHENTICATE {:form-params  form
                                :cookie-store cookie-store}))

(defn- get-heiaheia-login-form
  [cookie-store]
  (let [response (client/get HH_LOGIN {:cookie-store cookie-store})
        page     (enlive/html-snippet (:body response))
        form     (parse-login-form page)]
    (if-let [token (get form "authenticity_token")]
      form
      (throw (Exception. "Could not find HeiaHeia login form.")))))

(defn- create-session-via-form
  [username password]
  (let [cookie-store (clj-http.cookies/cookie-store)
        login-form   (get-heiaheia-login-form cookie-store)
        response     (submit-heiaheia-login-form (assoc login-form
                                                        "user[email]" username
                                                        "user[password]" password)
                                                 cookie-store)]
    (if (= (:status response) 302)
      {:token   (get login-form "authenticity_token")
       :cookies cookie-store}
      (throw (Exception. "Incorrect HeiaHeia credentials")))))

;;
;; Public functions
;;

(defn create-session
  [{:keys [username password type] :or {type :heiaheia}}]
  (case type
    :heiaheia (create-session-via-form username password)
    :facebook (create-session-via-facebook username password)
    (throw (Exception. "Unknown HeiaHeia login type."))))

(defn valid-credentials?
  [credentials]
  (try
    (create-session credentials)
    true
    (catch Exception e
      false)))

(defn create-workout
  [session workout]
  (let [duration (parse-duration (:duration workout))
        response (client/post HH_WORKOUTS {:form-params  {"utf8"                     "✓"
                                                          "authenticity_token"       (:token session)
                                                          "training_log[sport]"      (get HH_SPORTS (:sport workout) 14)
                                                          "training_log[date]"       (format-date (:date workout))
                                                          "training_log[duration_h]" (:h duration)
                                                          "training_log[duration_m]" (:m duration)
                                                          "training_log[duration_s]" (:s duration)
                                                          "training_log[distance]"   (get-or workout :distance 0)
                                                          "training_log[calories]"   (get-or workout :calories 0)
                                                          "training_log[avg_hr]"     (get-or workout :heart-rate-avg 0)
                                                          "training_log[max_hr]"     (get-or workout :heart-rate-max 0)
                                                          "training_log[favourite]"  false
                                                          "button"                   ""}
                                           :cookie-store (:cookies session)})]
    (= (:status response) 200)))
