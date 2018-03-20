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

(defn build-string [variables delim path]
  (str/join delim (map #(if (string? %) % (get variables %)) path)))

(defn build-file-string [variables path]
  (build-string variables "/" path))

(defn get-file-contents [variables path]
  (slurp (build-file-string variables path)))

(defn read-properties [^Properties properties]
  (reduce
    #(assoc %1 (keyword "prop" %2) (.getProperty properties %2))
    {}
    (.stringPropertyNames properties)))

(defmulti resolve-function (fn [_ [func & _]] func))

(defmethod resolve-function :default [_ [func & _]]
  (println func))

(defmethod resolve-function 'file/properties
  [{:keys [variables]} [_ & args]]
  (let [properties (Properties.)
        _
        (.load properties
               (ByteArrayInputStream.
                 (.getBytes
                   (get-file-contents variables args))))]
    (read-properties properties)))

(defmethod resolve-function 'file/xml
  [{:keys [variables]} [_ & args]]
  (zip/xml-zip
    (xml/parse
      (ByteArrayInputStream.
        (.getBytes
          (get-file-contents variables args))))))

(defmethod resolve-function 'file/json
  [{:keys [variables]} [_ & args]]
  (json/read-str (get-file-contents variables args) :key-fn keyword))

(defmethod resolve-function 'file/text
  [{:keys [variables]} [_ & args]]
  (get-file-contents variables args))

(defmethod resolve-function 'file/csv
  [{:keys [variables]} [_ & args]]
  (csv/read-csv (get-file-contents variables args)))

(defmethod resolve-function 'text
  [{:keys [variables]} [_ & args]]
  (build-string variables "" args))

(defn- resolve-value [context variables value]
  (cond
    (list? value) (resolve-function context value)
    (keyword? value) (get variables value)
    :else value))

(defmulti process-command (fn [_ [command & _]] command))

(defmethod process-command :default [out step]
  (update out :skips conj step))

(defmethod process-command 'properties
  [{:keys [variables] :as ctx} [_ & [value]]]
  (let [outval (resolve-value ctx variables value)]
    (update ctx :variables merge variables outval)))

(defmethod process-command 'assign
  [{:keys [variables] :as context} [_ & [variable value]]]
  (let [outval (resolve-value context variables value)]
    (update context :variables assoc variable outval)))

(defmethod process-command 'db-config
  [{:keys [variables] :as context} [_ & [value]]]
  (let [outval (resolve-value context variables value)]
    (assoc context :db ())))

(defmethod process-command 'db-tables
  [context [_ & args]]
  (update context :tables concat args))

(defn process-test [test-data]
  (let [{:keys [results skips]}
        (reduce
          process-command
          {:results []
           :skips []
           :variables
           (reduce-kv
             #(assoc %1 (keyword "env" %2) %3)
             (read-properties (System/getProperties))
             (System/getenv))}
          test-data)]
    [results skips]))

(defn run-regression [test-file]
  (println "Running test: " test-file)
  (let [test-data (edn/read-string (str "[" (slurp test-file) "]"))
        errors (s/explain-data :fault/testfile test-data)]
    (when-not (nil? errors)
      (throw (ExceptionInfo. "Test file cannot be read"
                             {:file test-file :errors errors})))
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
