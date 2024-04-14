(ns crosswords-svg
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.edn :as edn]
   [selmer.parser :as sp]
   [babashka.fs :as fs]))

(defn- ensure-dir [dir]
  (when-not (fs/exists? dir)
    (fs/create-dirs dir)))

(def build-dir "build")
(def gen-dir "gen")

(defn- clean []
  (fs/delete-tree build-dir)
  (fs/delete-tree gen-dir)
  (ensure-dir build-dir)
  (ensure-dir (str gen-dir "/js"))
  (ensure-dir (str gen-dir "/google-sync")))

(defn- ensure-build-dir [] (ensure-dir build-dir))
(defn- ensure-gen-dir [] (ensure-dir "gen/js"))

(defn- extract-puzzle-data [dir]
  (->>
   dir
   io/file
   file-seq
   (filter #(.isFile %1))
   (map
    (fn [f] {:name (.getName f) ;; remove .edn
             :data (edn/read-string (slurp f))}))))

(defn- save-and-forward [path content]
  (spit path content)
  content)

(defn- gen-index [puzzles]
  (let [template "./index.html.template"
        index-js (slurp "index.js")
        allcode (str "\n" puzzles "\n" index-js)]
    (->>
     (sp/render-file template {:env :build
                               :allcode allcode})
     (spit (str build-dir "/index.html")))))

(defn gen-index-dev []
  (spit "index.html"
        (sp/render-file "./index.html.template" {:env :dev})))

(defn gen-puzzles-js [ps]
  (let [template "./puzzles.js.template"]
    (sp/render-file template {:puzzle-count 1000 :puzzles ps})))

(defn gen-puzzle-data [puzzle-data-dir js-target-file]
  (->>
   (extract-puzzle-data puzzle-data-dir)
   (gen-puzzles-js)
   (save-and-forward js-target-file)))

(defn build-app []
  (ensure-build-dir)
  (ensure-gen-dir)
  (-> (gen-puzzle-data "gen/google-sync" "gen/js/puzzles.js") gen-index))

(defn do-clean [& args] (clean))

(defn -main [& args]
  (build-app))

(comment

  (->>
   (extract-puzzle-data "data/csv")
   (gen-puzzles-js))

  (build-app)

  (sp/render "{{ puzzles }}" {:puzzles (extract-puzzle-data "data/csv")})
;;
  )
