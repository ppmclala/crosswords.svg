(ns google-sync
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.tools.build.api :as build]
   [clojure.tools.logging :as log])
  (:import
   [com.google.api.client.googleapis.javanet GoogleNetHttpTransport]
   [com.google.api.client.json.gson GsonFactory]
   [com.google.api.services.sheets.v4 Sheets$Builder SheetsScopes]
   [com.google.auth.http HttpCredentialsAdapter]
   [com.google.auth.oauth2 GoogleCredentials]))

(def scopes [SheetsScopes/SPREADSHEETS SheetsScopes/DRIVE SheetsScopes/DRIVE_FILE])
;; TODO: have a test sheet and a public sheet driven by config
(def sheet-id "1McNmVbWzZL_twxwTR8Izh81VHRAQ0_sg_Y1KiPeJQfc")
(def client (atom nil))

;; TODO: add integrant hooks to bounce the google client

(defn ->credentials []
  (-> (GoogleCredentials/getApplicationDefault) (.createScoped scopes)))

(defn- ->sheets-client [creds]
  (try
    (->
     (Sheets$Builder.
      (GoogleNetHttpTransport/newTrustedTransport)
      (GsonFactory/getDefaultInstance)
      (HttpCredentialsAdapter. creds))
     (.setApplicationName "crosswords.svg")
     (.build))
    (catch Exception e
      (log/error e "Unable to register google sheets client"))))

(defn- refresh-sheets-client []
  (->>
   (->credentials)
   ->sheets-client
   (reset! client)))

(defmacro with-client [body]
  `(do (refresh-sheets-client) ~body))

(defn ->range [title] (str title "!A1:D83"))

(defn- ->clue [row]
  (zipmap [:id :direction :clue :answer] row))

(defn- ->edn [rvs]
  (->>
   (get rvs "values")
   (rest) ;; skip header
   (map #(->clue %))))

(defn- ->titles [sheets]
  (map #(get-in % ["properties" "title"]) sheets))

(defn- get-spreadsheet-values [s-id title]
  (-> @client
      (.spreadsheets)
      (.values)
      (.get s-id (->range title))
      (.execute)
      (->edn)))

(defn- ->sheet-data [s-id sheets title]
  (assoc sheets
         title
         (get-spreadsheet-values s-id title)))

(defn- list-pages [sheet-id]
  (-> @client
      (.spreadsheets)
      (.get sheet-id)
      (.execute)
      (get "sheets")
      (->titles)))

(defn sanitize-title [t]
  (-> t
      (str/replace #"\s+" "_")
      (str/replace #"[^a-zA-Z0-9_-]" "")))

(defn write-edn-file [r m k v]
  (let [out-file (str r "/" (sanitize-title k) ".edn")]
    (println "writing file" out-file)
    (spit out-file (into [] v))
    (assoc m k out-file)))

(defn- write-data [gen-root data]
  (reduce-kv (partial write-edn-file gen-root) {} data))

(defn- sync-sheets [gen-root s-id]
  (->>
   (list-pages s-id)
   (reduce (partial ->sheet-data s-id) {})
   (write-data gen-root)))

(defn dump-creds [& args]
  (let [adc (GoogleCredentials/getApplicationDefault)
        at (.getAccessToken adc)
        scoped (.createScoped adc scopes)
        scoped-at (.getAccessToken scoped)]
    (.refreshAccessToken adc)
    (.refreshAccessToken scoped)
    (println "post-refresh:")
    (println "ADC:" adc)
    (println "ADC.accessToken:" at)
    (println "scoped:" scoped)
    (println "scoped.accessToken:" scoped-at)))

(defn -main [& _]
  (println "Generating new puzzles from Google Sheets...")
  ;; TODO: use the ensure-dir code (just consolidate into build.clj)
  (let [gen-dir (io/as-file "gen")
        root-dir (io/as-file "gen/google-sync")]
    (when
     (and (.exists gen-dir) (.exists root-dir))
      (do
        (println "cleaning google sync dir" (.getPath root-dir))
        (build/delete {:path "gen/google-sync"})))

    (io/make-parents "gen/google-sync/foo.txt")

    (with-client
      (sync-sheets "gen/google-sync" sheet-id))
    (println "Done generating puzzle data in gen/google-sync")))

(comment

  (refresh-sheets-client)

  (list-pages sheet-id)

  (-main)

  (sanitize-title "foo#bar &|'baz")

  ;;
  )
