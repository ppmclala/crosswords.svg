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
(def sheet-id "1McNmVbWzZL_twxwTR8Izh81VHRAQ0_sg_Y1KiPeJQfc")

(defn ->credentials [path]
  (let [cred (GoogleCredentials/fromStream (io/input-stream path))
        scoped-cred (.createScoped cred scopes)]
    scoped-cred))

(def client (atom nil))

(defn- ->sheets [creds]
  (try
    (-> (Sheets$Builder.
         (GoogleNetHttpTransport/newTrustedTransport)
         (GsonFactory/getDefaultInstance)
         (HttpCredentialsAdapter. creds))
        (.setApplicationName "foo")
        (.build))
    (catch Exception e
      (log/error e "Unable to register google sheets client"))))

(defn- make-sheets-client []
  (->>
   (->secret :google-sa-creds-path)
   ->credentials
   ->sheets
   (reset! client)))

(defn- ->csv [range-values]
  (->>
   (get range-values "values")
   (reduce (fn [rows row] (conj rows (str/join "," row))) [])
   (str/join "\n")))

(defn- sync-sheets [gen-root]
  (let [p "reference_puzzle"]
    (spit
     (str gen-root "/" p ".csv")
     (-> @client
         (.spreadsheets)
         (.values)
         (.get sheet-id "reference_puzzle!A1:D83")
         (.execute)
         (->csv)))))

(defn -main [& args]
  (println "google-sync")
  (let [build-dir (io/as-file "build")
        root-dir (io/as-file "build/google-sync")]
    (when
     (and (.exists build-dir) (.exists root-dir))
      (build/delete {:path "build/google-sync"}))

    (io/make-parents "build/google-sync/foo.txt")

    (make-sheets-client)
    (sync-sheets "build/google-sync")))

(comment

  (-> (->secret :google-sa-creds-path) (->credentials))

  (make-sheets-client)
  (sync-sheets "")
  (-main)

  (-> @client
      (.spreadsheets)
      (.values)
      (.get sheet-id "reference_puzzle!A1:D83")
      (.execute))

  (-main)
  ;;
  )
