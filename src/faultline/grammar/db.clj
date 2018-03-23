(ns faultline.grammar.db
  (:require [clojure.spec.alpha :as s]
            [faultline.grammar.common :as c]
            [faultline.grammar.file :refer :all]
            [faultline.grammar.data :refer :all]
            [faultline.commmon.validation :as v]
            [pred-i-kit.core :as p]))

(s/def :db/table-name (s/and symbol? (p/named-as c/valid-name)))

(s/def :db/column-name (s/and symbol? (p/named-as c/valid-name)))

(s/def :db/classname :variable/string)

(s/def :db/url :variable/string)

(s/def :db/username :variable/string)

(s/def :db/password :variable/string)

(s/def :db/max-pool-size :variable/int)

(s/def :db/pool-provider #{"hikari" "tomcat" "c3p0"})

(s/def :db/table-keys (s/or :single :db/column-name
                            :multi (s/and vector?
                                          (p/min-count 2)
                                          (s/coll-of :db/column-name))))

(s/def :db/config
  (s/and list?
         (s/cat :label #{'db/config}
                :body (s/or :variable :fault/variable
                            :file :file/properties
                            :raw (s/keys :req-un [:db/classname :db/url :db/username :db/password :db/max-pool-size :db/pool-provider])))))

(s/def :db/tables (s/cat :label #{'db/tables}
                         :table (s/+ :db/table-name)))

(s/def :db/load-or-clean
  (s/cat :table :db/table-name
         :content (s/or :file-csv :file/csv
                        :file-json :file/json
                        :file-xml :file/xml
                        :file-yaml :file/yaml
                        :raw :data/table)))

(s/def :db/load
  (s/cat :label #{'db/load}
         :content :db/load-or-clean))

(s/def :db/clean
  (s/cat :label #{'db/clean}
         :content :db/load-or-clean))

(s/def :validation/rule
  (s/and list?
         (s/or :constant (s/cat :label '#{is-not-null})
               :unary (s/cat :label '#{< > not= <= >= like}
                             :value :data/primitive)
               :binary (s/cat :label '#{between}
                              :left :data/primitive
                              :right :data/primitive)
               :unary (s/cat :label '#{in}
                             :args (s/+ :data/primitive)))))

(s/def :table/validate
  (s/and
    list?
    (v/match-row-lengths)
    (s/cat :label #{'validate}
           :column-headers (s/and vector?
                                  (s/coll-of :db/table-name))
           :rows (s/+ (s/and vector?
                             (s/coll-of (s/or :literal :data/primitive
                                              :rule :validation/rule)))))))

(s/def :db/data-empty (s/and list? (s/cat :elem #{'EMPTY})))

(s/def :db/match-data (s/or :file :file/csv
                            :table :data/table
                            :validate :table/validate))

(s/def :db/state (s/and list?
                        (s/cat :label #{'db/state}
                               :query (s/or :simple string?
                                            :variable :fault/variable
                                            :complex (s/and vector?
                                                            :fault/templated-string))
                               :match-data (s/or :data :db/match-data
                                                 :empty :db/data-empty))))

(s/def :db/created
  (s/and list?
         (s/cat :label #{'db/created}
                :table-name :db/table-name
                :match-data (s/or :data :db/match-data
                                  :empty :db/data-empty))))

(s/def :db/deleted
  (s/and list?
         (s/cat :label #{'db/deleted}
                :table-name :db/table-name
                :match-data (s/or :data :db/match-data
                                  :empty :db/data-empty))))

(s/def :db/updated
  (s/and list?
         (s/or :empty (s/cat :label #{'db/updated}
                             :table-name :db/table-name
                             :value :db/data-empty)
               :compare (s/cat :label #{'db/updated}
                               :table-name :db/table-name
                               :previous-state :db/match-data
                               :current-state :db/match-data))))
