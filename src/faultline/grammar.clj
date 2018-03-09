(ns faultline.grammar
  (:require [clojure.spec.alpha :as s]
            [faultline.validation :as v]))

(def valid-name #"[a-zA-Z][a-zA-Z0-9_-$?]*")

(s/def :fault/variable (s/and keyword? (v/named-as valid-name)))

(s/def :fault/testfile (s/cat :imports (s/? :fault/import)
                              :setup (s/* :fault/setup)
                              :suites (s/+ :suite)
                              :teardown (s/* :fault/teardown)))

(s/def :suite/name (s/and symbol (v/named-as valid-name)))

(s/def :fault/suite (s/and list?
                           (s/cat :label #{'suite}
                                  :test-name :suite/name
                                  :setup (s/* :fault/setup)
                                  :test-or-suite (s/or (s/+ :fault/suite)
                                                       (s/+ :fault/test))
                                  :teardown (s/* :fault/teardown))))

(s/def :test/name (s/and symbol (v/named-as valid-name)))

(s/def :fault/test (s/and list?
                          (s/cat :label #{'test}
                                 :test-name :test/name
                                 :setup (s/* :fault/setup)
                                 :preconditions (s/* :fault/precondition)
                                 :request :test/request
                                 :response :test/response
                                 :postconditions (s/* :fault/postcondition)
                                 :teardown (s/* :fault/teardown))))

(s/def :fault/import (s/cat :properties (s/* (s/or :assign-or-comment :fault/assign-or-comment
                                                   :props :fault/properties))
                            :db-config (s/? :db/config)
                            :db-schema (s/* (s/or :assign-or-comment :fault/assign-or-comment
                                                  :schema :fault/db-schema))))

(s/def :fault/setup (s/or :db-load :db/load
                          :assign-or-comment :fault/assign-or-comment))

(s/def :fault/teardown (s/or :db-clean :db/clean
                             :comment :fault/comment))

(s/def :fault/precondition (s/or :comment :fault/comment))

(s/def :fault/postcondition (s/or :db-created :db/created
                                  :db-updated :db/updated
                                  :db-deleted :db/deleted
                                  :comment :fault/comment))

(s/def :fault/assign-or-comment (s/or :assign :fault/assign
                                      :comment :fault/comment))

(s/def :fault/comment string?)

(s/def :fault/assign (s/and list?
                            (s/cat :label #{'assign}
                                   :variable :fault/variable
                                   :value :config/value)))

(s/def :config/value
  (s/or :complex :data/structured
        :variable :fault/variable
        :text string?
        :number number?
        :boolean boolean?))

(s/def :data/structured (s/and list?
                               (s/or :file (s/cat :label (v/named-as #{'file} '#{xml json text yaml csv})
                                                  :file-name :fault/templated-string)
                                     :json (s/cat :label #{'json}
                                                  :body :body/json)
                                     :xml (s/cat :label #{'xml}
                                                 :body :body/xml)
                                     :plain-text (s/cat :label #{'text}
                                                        :body :fault/templated-string))))

(s/def :fault/properties (s/and list?
                                (s/cat :label #{'properties}
                                       :file :fault/templated-string)))

(s/def :fault/templated-string (s/or :simple string?
                                     :variable (s/and vector?
                                                      (s/coll-of (s/or :string string?
                                                                       :variable :fault/variable)))))

(s/def :test/request
  (s/and list?
         (s/cat :label #{'request}
                :method :request/method
                :url :request/url
                :headers (s/? :endpoint/headers)
                :body (s/? :endpoint/body))))

(s/def :request/url (s/or :simple string?
                          :variable :fault/variable
                          :complex (s/and vector?
                                           (s/cat :protocal #{"http://" "https://"}
                                                  :steps (s/+ (s/or :string string?
                                                                    :variable :fault/variable))))))

(s/def :request/method '#{GET POST PUT DELETE HEAD OPTIONS CONNECT PATCH})

(s/def :endpoint/headers (s/map-of string? (s/or :primitive string?
                                                 :variable :fault/variable)))

(s/def :endpoint/body (s/or :simple string?
                        :complex (s/and list?
                                        (s/or :file (s/cat :label #{'file}
                                                           :type '#{xml json text}
                                                           :file-name :fault/templated-string)
                                              :json (s/cat :label #{'json}
                                                           :body :body/json)
                                              :xml (s/cat :label #{'xml}
                                                          :body :body/xml)
                                              :plain-text (s/cat :label #{'text}
                                                                 :body :fault/templated-string)))))

(s/def :body/json (s/or :array (s/and vector? (s/coll-of :body/json))
                        :map (s/map-of string? :body/json)
                        :primitive (s/or :string string?
                                         :boolean boolean?
                                         :nil nil?
                                         :int int?
                                         :decimal decimal?
                                         :variable :fault/variable)))

(def xml-valid-name #"[a-wyzA-WYZ_][a-zA-Z0-9_-\.]*")

(s/def :xml/name (s/and symbol (s/or :namespace (v/named-as xml-valid-name xml-valid-name)
                                     :name-only (v/named-as xml-valid-name))))

(s/def :body/xml (s/and vector? (s/cat :name :xml/name
                                       :attrs (s/? (s/map-of :xml/name (s/or :primitive string?
                                                                             :variable :fault/variable)))
                                       :children (s/* (s/or :node :body/xml
                                                            :text string?
                                                            :variable :fault/variable)))))
(s/def :response/status (v/one-of (concat (range 100 104) (range 200 209) [226] (range 300 309) (range 400 419) (range 421 427) [428 429 431 451] (range 500 509) [510 511])))

(s/def :test/response
  (s/and list?
         (s/cat :label #{'response}
                :status :response/status
                :headers (s/? :endpoint/headers)
                :body (s/? :endpoint/body))))

;===========================================================================================================

(s/def :test/pre (s/and vector?
                        (s/+ (s/or :config :fault/config
                                   :db-config (s/and list?
                                                     (s/cat :label #{'db-config}
                                                            :body :db/config))
                                   :db-load (s/and list?
                                                   (s/cat :label #{'db-load}
                                                          :body :db/load))))))

(s/def :db/config (s/keys :req-un [:db/classname :db/url :db/username :db/password :db/max-pool-size :db/pool-provider :db/table-keys]))

(s/def :variable/string (s/or :literal string?
                              :variable :fault/variable))

(s/def :variable/int (s/or :literal int?
                           :variable :fault/variable))

(s/def :db/classname :variable/string)

(s/def :db/url :variable/string)

(s/def :db/username :variable/string)

(s/def :db/password :variable/string)

(s/def :db/max-pool-size :variable/int)

(s/def :db/pool-provider #{"hikari" "tomcat" "c3p0"})

(s/def :db/table-keys (s/map-of :db/entity-name
                                (s/or :single :db/entity-name
                                      :multi (s/and vector?
                                                    (v/min-count 1)
                                                    (s/coll-of :db/entity-name)))))

(s/def :db/entity-name (s/and symbol? (v/named-as #"[a-zA-Z][a-zA-Z0-9_-$]*")))

(s/def :db/record (s/map-of :db/entity-name (s/or :text string?
                                                  :var :fault/variable)))

(s/def :db/load (s/or
                  :variable :fault/variable
                  :tables (s/or :file :file/not-csv
                                :tables (s/map-of :db/entity-name
                                                  (s/or :file :file/with-csv
                                                        :variable :fault/variable
                                                        :records (s/or :single :db/record
                                                                       :multi (s/and vector?
                                                                                     (s/coll-of :db/record)))
                                                        :sample (s/and list?
                                                                       (s/cat :label #{'sample}
                                                                              :data :load/sample)))))
                  :sample (s/and list?
                                 (s/cat :label #{'sample}
                                        :data (s/or :file :file/not-csv
                                                    :raw (s/map-of :db/entity-name
                                                                   :load/sample))))))

(s/def :load/sample (s/or :file :file/with-csv
                          :raw (s/map-of :db/entity-name
                                         (s/and set?
                                                (s/coll-of string?)))))

(s/def :file/not-csv (s/and list?
                            (s/cat :label #{'file}
                                   :type '#{json edn yaml}
                                   :file-name :fault/templated-string)))

(s/def :file/with-csv (s/and list?
                            (s/cat :label #{'file}
                                   :type '#{json edn yaml csv}
                                   :file-name :fault/templated-string)))

(s/def :test/post
  ;todo
  any?)
