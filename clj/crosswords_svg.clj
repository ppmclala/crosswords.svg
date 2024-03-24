(ns crosswords-svg
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [selmer.parser :as sp]))

(defn extract-puzzle-data [dir]
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

(defn gen-puzzles-js [ps]
  (let [template "./puzzles.js.template"]
    (sp/render-file template {:puzzle-count 1000 :puzzles ps})))

(defn -main [& args]
  (->> 
    (extract-puzzle-data "data/csv")
    (gen-puzzles-js)
    (spit "gen/js/puzzles.js")))


(comment

  (->>
    (extract-puzzle-data "data/csv")
    (gen-puzzles-js)
    (spit "gen/js/puzzles.js"))

  (sp/render "{{ puzzles }}" {:puzzles (extract-puzzle-data "data/csv")})
;;
  )
