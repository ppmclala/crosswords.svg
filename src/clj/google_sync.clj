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

(def secrets-path (str (System/getProperty "user.home") "/.secrets.edn"))

(defn ->secret [k]
  (when (.exists (io/as-file secrets-path))
    (->
     (slurp secrets-path)
     edn/read-string
     (get k))))

(def scopes [SheetsScopes/SPREADSHEETS SheetsScopes/DRIVE SheetsScopes/DRIVE_FILE])
;; TODO: have a test sheet and a public sheet driven by config
(def sheet-id "1McNmVbWzZL_twxwTR8Izh81VHRAQ0_sg_Y1KiPeJQfc")
(def client (atom nil))

;; TODO: add integrant hooks to bounce the google client

;; TODO: this can be removed once we verify on GH Action
(defn ->credentials [path]
  ;;(GoogleCredentials/fromStream (io/input-stream path))
  (let [cred (GoogleCredentials/getApplicationDefault)
        scoped-cred (.createScoped cred scopes)]
    (println "(type cred):" (type cred))
    (println "(type scoped-cred):" (type scoped-cred))
    scoped-cred))

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
   (->secret :google-sa-creds-path)
   ->credentials
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
  (let [build-dir (io/as-file "build")
        root-dir (io/as-file "build/google-sync")]
    (when
     (and (.exists build-dir) (.exists root-dir))
      (do
        (println "cleaning google sync dir" (.getPath root-dir))
        (build/delete {:path "build/google-sync"})))

    (io/make-parents "build/google-sync/foo.txt")

    (with-client
      (sync-sheets "build/google-sync" sheet-id))
    (println "Done generating puzzle data in build/google-sync")))

(comment

  ;; TODO: credentials for GH action
  (-> (->secret :google-sa-creds-path) (->credentials))

  (refresh-sheets-client)

  (list-pages sheet-id)

  (-main)

  (sanitize-title "foo#bar &|'baz")

  ;;
  )
