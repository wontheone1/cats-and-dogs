(ns cats-and-dogs.preprocess
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [mikera.image.core :as imagez]
            [mikera.image.filters :as filters]))

(defn- preprocess-image
  "scale to image-size and convert the picture to grayscale"
  [output-dir image-size [idx [file label]]]
  (let [img-path (str output-dir "/" label "/" idx ".png")]
    (when-not (.exists (io/file img-path))
      (println "> " img-path)
      (io/make-parents img-path)
      (-> (imagez/load-image file)
          ((filters/grayscale))
          (imagez/resize image-size image-size)
          (imagez/save img-path)))))

(defn- gather-all-files-in-directory [path]
  (->> (io/file path)
       (file-seq)
       (filter #(.isFile %))))

(defn- produce-indexed-data-label-seq [files]
  (->> files
       (map (fn [file]
              [file (-> file (.getName) (string/split #"\.") first)]))
       (map-indexed vector)))

; use first half of the files for training
; and second half for testing
(defn- build-image-data
  [original-data-dir training-dir testing-dir target-image-size]
  (let [files (gather-all-files-in-directory original-data-dir)
        pfiles (partition (int (/ (count files) 2)) (shuffle files))
        training-observation-label-seq (produce-indexed-data-label-seq (first pfiles))
        testing-observation-label-seq (produce-indexed-data-label-seq (last pfiles))
        put-processed-image-to-train-dir (partial preprocess-image training-dir target-image-size)
        put-processed-image-to-test-dir (partial preprocess-image testing-dir target-image-size)]
    (dorun (pmap put-processed-image-to-train-dir training-observation-label-seq))
    (dorun (pmap put-processed-image-to-test-dir testing-observation-label-seq))))

(defn run-preprocess []
  (build-image-data
    "data-cats-dogs/original"
    "data-cats-dogs/training"
    "data-cats-dogs/testing"
    52))
