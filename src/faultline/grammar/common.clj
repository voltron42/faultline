(ns faultline.grammar.common
  (:require [clojure.spec.alpha :as s]
            [faultline.commmon.validation :as v]
            [pred-i-kit.core :as p]))

(def valid-name #"[a-zA-Z][a-zA-Z0-9_-]*")

(s/def :fault/tag-name (s/and symbol? (p/named-as valid-name)))

(s/def :fault/variable (s/and keyword? (p/named-as valid-name)))

(s/def :variable/string (s/or :literal string?
                              :variable :fault/variable))

(s/def :variable/int (s/or :literal int?
                           :variable :fault/variable))

(s/def :fault/templated-string (s/+ :variable/string))

