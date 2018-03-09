(ns faultline.core-test
  (:require [clojure.test :refer :all]
            [faultline.core :refer :all]
            [clojure.edn :as edn]))

(deftest test-sample
  (let [data (edn/read-string (str "[" (slurp "resources/sample.edn") "]"))]
    (println (type data))
    (println (count data))
    (println (type (first data)))))