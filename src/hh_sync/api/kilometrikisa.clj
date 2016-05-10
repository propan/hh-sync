(ns hh-sync.api.kilometrikisa
  (:require [clj-http.client :as client]
            [net.cgrand.enlive-html :as enlive])
  (:import [java.lang Math]))

(def KS_LOGIN "https://www.kilometrikisa.fi/accounts/login/")
(def KS_LOG   "https://www.kilometrikisa.fi/contest/log/")
(def KS_LOG_SAVE "https://www.kilometrikisa.fi/contest/log-save/")
(def KS_LOG_LIST "https://www.kilometrikisa.fi/contest/log_list_json/")
(def KS_DATE_FORMAT "yyyy-MM-dd")

(defn- parse-distance
  [distance]
  (try
    (java.lang.Float/parseFloat distance)
    (catch Exception e
      0.0)))

(defn- parse-login-form
  [page]
  (->> (enlive/select page [:form :input])
       (map :attrs)
       (filter :name)
       (map #(vector (:name %) (:value %)))
       (into (hash-map))))

(defn- get-login-form
  [cookie-store]
  (let [response (client/get KS_LOGIN {:cookie-store cookie-store})
        page     (enlive/html-snippet (:body response))
        form     (parse-login-form page)]
    (if-let [token (get form "csrfmiddlewaretoken")]
      form
      (throw (Exception. "Could not find Kilometrikisa login form.")))))

(defn- submit-login-form
  [form cookie-store]
  (client/post KS_LOGIN {:form-params  form
                         :headers      {"referer" KS_LOGIN}
                         :cookie-store cookie-store}))

(defn- find-csfr-token
  [session]
  (when-let [cookie (->> session
                         (:cookies)
                         (.getCookies)
                         (filter #(= (.getName %) "csrftoken"))
                         (first))]
    (.getValue cookie)))

;;
;; Public functions
;;

(defn create-session
  [{:keys [username password contest-id]}]
  (let [cookie-store (clj-http.cookies/cookie-store)
        login-form   (get-login-form cookie-store)
        response     (submit-login-form (assoc login-form
                                               "username" username
                                               "password" password)
                                        cookie-store)]
    (if (= (:status response) 302)
      {:token      (get login-form "csrfmiddlewaretoken")
       :contest-id contest-id
       :cookies    cookie-store}
      (throw (Exception. "Incorrect Kilometrikisa credentials")))))

(defn valid-credentials?
  [credentials]
  (try
    (create-session credentials)
    true
    (catch Exception e
      false)))

(defn format-date
  [date]
  (-> KS_DATE_FORMAT (java.text.SimpleDateFormat.) (.format date)))

(defn get-logs
  [session start end]
  (let [response (client/get (str KS_LOG_LIST (:contest-id session) "/")
                             {:query-params {:start (/ (.getTime start) 1000)
                                             :end   (/ (.getTime end) 1000)}
                              :headers      {"referer" KS_LOG}
                              :cookie-store (:cookies session)
                              :as           :json})]
    (if (= (:status response) 200)
      (reduce #(assoc %1 (:start %2) (parse-distance (:title %2))) {} (:body response))
      (throw (Exception. "Failed to fetch existing entries from Kilometrikisa.")))))

(defn save-day-stats
  [session date amount]
  (let [response (client/post KS_LOG_SAVE {:form-params  {"contest_id"          (:contest-id session)
                                                          "km_amount"           (-> (java.text.DecimalFormat. "#.##") (.format amount))
                                                          "km_date"             date
                                                          "csrfmiddlewaretoken" (or (find-csfr-token session)
                                                                                    (:token session))}
                                           :headers      {"referer" KS_LOG}
                                           :cookie-store (:cookies session)})]
    (= (:status response) 200)))

