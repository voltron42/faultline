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
           (java.util.Map Entry)
           (java.util Properties)))

(defn build-next [db tables variables results]
  {:db db :tables tables :variables variables :results results})

(defn build-string [variables delim path]
  (str/join delim (map #(if (string? %) % (get variables %)) path)))

(defn build-file-string [variables path]
  (build-string variables "/" path))

(defn get-file-contents [variables path]
  (slurp (build-file-string variables path)))

(def functions
  {'file/properties (fn [{:keys [variables args]}]
                      (let [properties (Properties.)]
                        (.load properties
                               (ByteArrayInputStream.
                                 (.getBytes
                                   (get-file-contents variables args))))))
   'file/xml (fn [{:keys [variables args]}]
               (zip/xml-zip
                 (xml/parse
                   (ByteArrayInputStream.
                     (.getBytes
                       (get-file-contents variables args))))))
   'file/json (fn [{:keys [variables args]}]
                (json/read-str (get-file-contents variables args)))
   'file/text (fn [{:keys [variables args]}]
                (get-file-contents variables args))
   'file/csv (fn [{:keys [variables args]}]
               (csv/read-csv
                 (get-file-contents variables args)))
   'text (fn [{:keys [variables args]}]
           (build-string variables "" args))
   'params (fn [{:keys [variables] [params] :args}]
             ;todo
             )
   'xml (fn [{:keys [variables] [xml-body] :args}]
          ;todo
          )
   'json (fn [{:keys [variables] [json-body] :args}]
           ;todo
           )
   'table (fn [{:keys [variables] rows :args}]
           ;todo
           )
   })

(def commands {'properties (fn [{:keys [db tables variables results args] :as context}]
                             ;todo
                             )
               'assign (fn [{:keys [variables] [variable value] :args :as context}]
                         (let [outval (cond
                                        (list? value) (let [[func & args] value]
                                                        ((functions func) (assoc context :args args)))
                                        (keyword? value) (get variables value)
                                        :else value)]
                           (assoc context :variables (assoc variables variable outval))))
               'tags (fn [context]
                       (dissoc context :args))
               'db-config (fn [{:keys [args] :as context}]
                            ;todo
                            )
               'db-tables (fn [{:keys [args] :as context}]
                            (assoc context :tables args))
               })

(defn process-test [test-data]
  (let [system-properties (System/getProperties)]
    (reduce
      (fn [{:keys [db tables variables results]} [command & args]]
        ((get commands command) db tables variables results args))
      {:db (atom nil)
       :tables (atom [])
       :variables (reduce
                    #(assoc %1 (keyword "prop" %2) (.getProperty system-properties %2))
                    (reduce
                      #(assoc %1 (keyword "env" (.getKey %2)) (.getValue %2))
                      {}
                      (.entrySet (System/getenv)))
                    (.propertyNames system-properties))
       :results []}
      test-data)))

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
