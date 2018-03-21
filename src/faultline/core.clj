(ns faultline.core
  (:require [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [faultline.grammar.core :refer :all]
            [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [org.httpkit.client :as client]
            [clojure.data.csv :as csv]
            [clojure.set :as set])
  (:import (java.io File ByteArrayInputStream)
           (clojure.lang ExceptionInfo)
           (java.util Properties)))


(defn build-db-conn-pool [conn]
  ; todo
  conn)

(defn read-properties [^Properties properties]
  (reduce
    #(assoc %1 (keyword "prop" %2) (.getProperty properties %2))
    {}
    (.stringPropertyNames properties)))

(defmulti resolve-function (fn [_ [func & _]] func))

(defn resolve-value [context variables value]
  (cond
    (list? value) (resolve-function context value)
    (keyword? value) (get variables value value)
    (map? value) (reduce-kv
                   #(assoc %1
                      (resolve-value context variables %2)
                      (resolve-value context variables %3))
                   {}
                   value)
    (vector? value) (mapv (partial resolve-value context variables) value)
    (= 'null value) nil
    :else value))

(defn build-string [ctx variables delim path]
  (str/join delim (map (partial resolve-value ctx variables) path)))

(defn build-file-string [ctx variables path]
  (build-string ctx variables "/" path))

(defn get-file-contents [ctx variables path]
  (slurp (build-file-string ctx variables path)))

; todo

(defmethod resolve-function :default [_ [func & _]]
  (println func))

(defmethod resolve-function 'file/properties
  [{:keys [variables] :as ctx} [_ & args]]
  (let [properties (Properties.)
        _
        (.load properties
               (ByteArrayInputStream.
                 (.getBytes
                   (get-file-contents ctx variables args))))]
    (read-properties properties)))

(defmethod resolve-function 'file/xml
  [{:keys [variables] :as ctx} [_ & args]]
  (zip/xml-zip
    (xml/parse
      (ByteArrayInputStream.
        (.getBytes
          (get-file-contents ctx variables args))))))

(defmethod resolve-function 'file/json
  [{:keys [variables] :as ctx} [_ & args]]
  (json/read-str (get-file-contents ctx variables args) :key-fn keyword))

(defmethod resolve-function 'file/text
  [{:keys [variables] :as ctx} [_ & args]]
  (get-file-contents ctx variables args))

(defmethod resolve-function 'file/csv
  [{:keys [variables] :as ctx} [_ & args]]
  (csv/read-csv (get-file-contents ctx variables args)))

(defmethod resolve-function 'text
  [{:keys [variables] :as ctx} [_ & args]]
  (build-string ctx variables "" args))

(defmulti process-command (fn [_ [command & _]] command))

(defn process-commands [context commands-and-comments]
  (reduce process-command context (filter list? commands-and-comments)))

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
    (assoc context :db (build-db-conn-pool outval))))

(defmethod process-command 'db-tables
  [context [_ & args]]
  (update context :tables concat args))

(defn super-structure [structure-type context label commands-and-comments]
  (let [new-ctx (assoc context :results [] :skips [])
        new-ctx (process-commands new-ctx commands-and-comments)]
    (update context :results conj
            (assoc (select-keys new-ctx [:results :skips])
              structure-type label))))

(defmethod process-command 'suite
  [context [_ suite-name & commands-and-comments]]
  (super-structure :suite context suite-name commands-and-comments))

(defmethod process-command 'test
  [context [_ test-name & commands-and-comments]]
  (super-structure :test context test-name commands-and-comments))

(defmethod process-command 'request
  [{:keys [variables] :as ctx} [_ method url headers & [body]]]
  (let [headers (resolve-value ctx variables headers)
        body (resolve-value ctx variables body)
        method (keyword (str/lower-case (name method)))
        url (build-file-string ctx variables url)
        req (merge {:method method
                    :url    url}
                   (if (empty? headers) {} {:headers headers})
                   (if (empty? body) {} {:body body}))
        resp (client/request req)]
    (assoc ctx :response resp)))

(defmulti compare (fn [compare-type _ _ _] compare-type))

(defmethod compare :status [_ {:keys [variables] :as ctx} actual expected]
  (let [expected (resolve-value ctx variables expected)])
  (when-not (= actual expected)
    {:actual actual :expected expected}))

(defmulti resolve-matcher )

(defmethod compare :headers [_ {:keys [variables] :as ctx} actual expected]
  (let [matcher (resolve-matcher expected)]
    ;todo
    ))

(defmethod compare :body [_ {:keys [variables] :as ctx} actual expected]

  ;todo
  )

(defmethod process-command 'response
  [{{:keys [status body headers]} :response :as ctx}
   [_ expected-status expected-headers & [expected-body]]]
  (let [errors (reduce-kv
                 #(if-let [error (apply compare %2 ctx %3)] (assoc %1 %2 error) %1)
                 {}
                 {:status [status expected-status]
                  :headers [headers expected-headers]
                  :body [body expected-body]})]
    (if-not (empty? errors)
      (update ctx :results conj errors)
      ctx)))

(defn process-test [test-data]
  (let [{:keys [results skips]}
        (process-commands
          {:results []
           :skips []
           :variables (reduce-kv
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
    (process-test test-data)))

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
