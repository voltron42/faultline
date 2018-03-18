(ns faultline.core
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [faultline.grammar.core :refer :all]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.csv :as csv])
  (:import (java.io File ByteArrayInputStream)
           (clojure.lang ExceptionInfo)
           (java.util Properties)))

(defn build-next [db tables variables results]
  {:db db :tables tables :variables variables :results results})

(defn build-string [variables delim path]
  (str/join delim (map #(if (string? %) % (get variables %)) path)))

(defn build-file-string [variables path]
  (build-string variables "/" path))

(defn get-file-contents [variables path]
  (slurp (build-file-string variables path)))

(defmulti resolve-function (fn [func & _] func))

(defmethod resolve-function :default [func context args]
  (println func))

(defmethod resolve-function 'file/properties
  [_ {:keys [variables]} args]
  (let [properties (Properties.)]
    (.load properties
           (ByteArrayInputStream.
             (.getBytes
               (get-file-contents variables args))))))

(defmethod resolve-function 'file/xml
  [_ {:keys [variables]} args]
  (zip/xml-zip
    (xml/parse
      (ByteArrayInputStream.
        (.getBytes
          (get-file-contents variables args))))))

(defmethod resolve-function 'file/json
  [_ {:keys [variables]} args]
  (json/read-str (get-file-contents variables args)))

(defmethod resolve-function 'file/text
  [_ {:keys [variables]} args]
  (get-file-contents variables args))

(defmethod resolve-function 'file/csv
  [_ {:keys [variables]} args]
  (csv/read-csv (get-file-contents variables args)))

(defmethod resolve-function 'text
  [_ {:keys [variables]} args]
  (build-string variables "" args))

(defmulti process-command (fn [_ [command & _]] command))

(defmethod process-command :default [out step] out)

(defmethod process-command 'properties [{:keys [db tables variables results] :as ctx} [_ & args]]
  ;todo
  )

(defmethod process-command 'assign [{:keys [db tables variables results] :as context} [_ & [variable value]]]
  (let [outval (cond
                 (list? value) (let [[func & args] value]
                                 (resolve-function func context args))
                 (keyword? value) (get variables value)
                 :else value)]
    (assoc context :variables (assoc variables variable outval))))

(defmethod process-command 'db-config [context [_ & args]]
  ;todo
  )

(defmethod process-command 'db-tables [context [_ & args]]
  (assoc context :tables args))

(defn process-test [test-data]
  (let [system-properties (System/getProperties)
        {:keys [db tables variables results]}
        (reduce
          process-command
          {:db (atom nil)
           :tables (atom [])
           :variables
           (reduce
             #(assoc %1 (keyword "prop" %2) (.getProperty system-properties %2))
             (reduce-kv
               #(assoc %1 (keyword "env" %2) %3)
               {}
               (System/getenv))
             (.propertyNames system-properties))
           :results []}
          test-data)]
    ))

(defn run-regression [test-file]
  (println "Running test: " test-file)
  (let [test-data (edn/read-string (str "[" (slurp test-file) "]"))
        errors (s/explain-data :fault/testfile test-data)]
    (when-not (nil? errors)
      (throw (ExceptionInfo. "Test file cannot be read" {:file test-file
                                 :errors errors})))
    (process-test (filter #(not (string? %)) test-data))))

(defn get-tests [file-list ^String test-file-or-folder]
  (let [file-obj (File. test-file-or-folder)]
    (if (.isDirectory file-obj)
      (reduce get-tests file-list (.list file-obj))
      (conj file-list test-file-or-folder))))

(defn print-results [test-results]
  (reduce #(println %2) nil test-results))

(defn print-exception [^ExceptionInfo e]
  (println e))

(defn -main [& test-files]
  (if (empty? test-files)
    (println "To run tests, please include test files or folders in the command")
    (doseq [test-file (reduce get-tests [] test-files)]
      (try
        (print-results (run-regression test-file))
        (catch ExceptionInfo e
          (print-exception e))))))
