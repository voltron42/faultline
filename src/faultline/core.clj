(ns faultline.core
  (:import (java.io File)))

(defn run-regression [test-file]
  (println "Running test: " test-file)
  ;todo
  )

(defn get-tests [file-list ^String test-file-or-folder]
  (let [file-obj (File. test-file-or-folder)]
    (if (.isDirectory file-obj)
      (reduce get-tests file-list (.list file-obj))
      (conj file-list test-file-or-folder))))

(defn -main [& test-files]
  (if (empty? test-files)
    (println "To run tests, please include test files or folders in the command")
    (doseq [test-file (reduce get-tests [] test-files)]
      (run-regression test-file))))