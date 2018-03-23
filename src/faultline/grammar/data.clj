(ns faultline.grammar.data
  (:require [clojure.spec.alpha :as s]
            [faultline.grammar.common :refer :all]
            [faultline.commmon.validation :as v]
            [pred-i-kit.core :as p]))

(def xml-valid-name #"[a-wyzA-WYZ_][a-zA-Z0-9_-]*")

(s/def :xml/name
  (s/and symbol?
         (s/or :namespace (p/named-as xml-valid-name xml-valid-name)
               :name-only (p/named-as xml-valid-name))))

(s/def :body/xml
  (s/and vector?
         (s/cat
           :name :xml/name
           :attrs (s/? (s/map-of :xml/name :variable/string))
           :children (s/* (s/or :node :body/xml
                                :text string?
                                :variable :fault/variable)))))

(s/def :data/xml
  (s/and list?
         (s/cat
           :label #{'xml}
           :content :body/xml)))

(s/def :data/primitive
  (s/or :string string?
        :boolean boolean?
        :null #{'null}
        :int int?
        :decimal decimal?
        :variable :fault/variable))

(s/def :body/json
  (s/or :array (s/and vector? (s/coll-of :body/json))
        :map (s/map-of string? :body/json)
        :primitive :data/primitive))

(s/def :data/json
  (s/and list?
         (s/cat
           :label #{'json}
           :content :body/json)))

(s/def :data/table
  (s/and
    list?
    (v/match-row-lengths)
    (s/cat :label #{'table}
           :column-headers (s/and vector?
                                  (s/coll-of :db/table-name))
           :rows (s/+ (s/and vector?
                             (s/coll-of :data/primitive))))))

(s/def :data/plain-text
  (s/and list?
         (s/cat :label #{'text}
                :body :fault/templated-string)))

(s/def :data/params
  (s/and list?
         (s/cat :label #{'params}
                :params (s/map-of :fault/http-param-name
                                  (s/or :single :variable/string
                                        :multi (s/and vector?
                                                      (s/coll-of :variable/string)))))))

(s/def :data/structured (s/or :json :data/json
                              :xml :data/xml
                              :params :data/params
                              :plain-text :data/plain-text))
