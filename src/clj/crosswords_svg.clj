(ns crosswords-svg
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [selmer.parser :as sp]
   [babashka.fs :as fs]))

(def build-dir "build")

(defn clean []
  (fs/delete-tree build-dir))

(defn- ensure-build-dir []
  (when-not (fs/exists? build-dir)
    (fs/create-dir build-dir)))

(defn- extract-puzzle-data [dir]
  (->>
   dir
   io/file
   file-seq
   (filter #(.isFile %1))
   (map
    (fn [f] {:name (.getName f)
             :data
             (let [csv-data (csv/read-csv (slurp f))]
               (map zipmap
                    (->> (first csv-data) (map keyword) repeat)
                    (rest csv-data)))}))))

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

(defn gen-puzzle-data []
  (->>
   (extract-puzzle-data "data/csv")
   (gen-puzzles-js)
   (save-and-forward "gen/js/puzzles.js")))

(defn- build-app []
  (clean)
  (ensure-build-dir)
  (-> (gen-puzzle-data) gen-index))

(defn -main [& args]
  (build-app))

(comment

  (->>
   (extract-puzzle-data "data/csv")
   (gen-puzzles-js))

  (build-app)

  (gen-puzzle-data)

  (sp/render "{{ puzzles }}" {:puzzles (extract-puzzle-data "data/csv")})
;;
  )
