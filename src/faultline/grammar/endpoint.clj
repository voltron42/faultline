(ns faultline.grammar.endpoint
  (:require [clojure.spec.alpha :as s]
            [faultline.grammar.common :refer :all]
            [faultline.grammar.file :refer :all]
            [faultline.grammar.data :refer :all]
            [faultline.commmon.validation :as v]
            [pred-i-kit.core :as p]))

(s/def :endpoint/headers (s/map-of string? :variable/string))

(s/def :request/method '#{GET POST PUT DELETE HEAD OPTIONS CONNECT PATCH})

(s/def :endpoint/body
  (s/or :simple string?
        :file :fault/file
        :data :data/structured))

(s/def :request/url
  (s/or :simple string?
        :variable :fault/variable
        :complex (s/and vector?
                        (p/min-count 2)
                        (s/cat :url :fault/templated-string
                               :params (s/? :data/params)))))

(s/def :test/request
  (s/and list?
         (s/cat :label #{'request}
                :method :request/method
                :url :request/url
                :headers :endpoint/headers
                :body (s/? :endpoint/body))))

(s/def :response/status (set (concat (range 100 104) (range 200 209) [226] (range 300 309) (range 400 419) (range 421 427) [428 429 431 451] (range 500 509) [510 511])))

(s/def :test/response
  (s/and list?
         (s/cat :label #{'response}
                :status :response/status
                :headers :endpoint/headers
                :body (s/? :endpoint/body))))
