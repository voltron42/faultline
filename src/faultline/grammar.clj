(ns faultline.grammar
  (:require [clojure.spec.alpha :as s]
            [faultline.validation :as v]))

(s/def :fault/regression (s/or :suite-only :fault/suites
                               :globals (s/and vector?
                                               (s/cat :imports-and-configs (s/+ (s/or :import :fault/import
                                                                                      :config :fault/config
                                                                                      :db-config :db-config))
                                                      :suite (s/or :single :fault/suite
                                                                   :multi :fault/suites)))))

(s/def :fault/import (s/and list?
                            (s/cat :label #{'import}
                                   :file :fault/templated-string)))

(s/def :fault/config (s/and list?
                            (s/cat :label #{'assign}
                                   :variable :fault/variable
                                   :value :config/value)))

(s/def :config/value
  (s/or :complex (s/and list?
                        (s/or :file (s/cat :label #{'file}
                                           :type '#{xml json text yaml csv}
                                           :file-name :fault/templated-string)
                              :json (s/cat :label #{'json}
                                           :body :body/json)
                              :xml (s/cat :label #{'xml}
                                          :body :body/xml)
                              :plain-text (s/cat :label #{'text}
                                                 :body :fault/templated-string)))
        :variable :fault/variable
        :text string?
        :number number?
        :boolean boolean?))

(def valid-name #"[a-zA-Z][a-zA-Z0-9_-$?]*")

(s/def :fault/variable (s/and keyword? (v/named-as valid-name)))

(s/def :fault/suites (s/map-of :suite/name :fault/suite))

(s/def :fault/tests (s/map-of :test/name :fault/test))

(s/def :fault/suite (s/merge (s/keys :opt-un [:test/pre :test/post])
                             (s/or
                               (s/keys :req-un [:fault/suites])
                               (s/keys :req-un [:fault/tests]))))

(s/def :suite/name (s/and symbol (v/named-as valid-name)))

(s/def :test/name (s/and symbol (v/named-as valid-name)))

(s/def :fault/test (s/merge (s/keys :opt-un [:test/pre :test/post])
                            (s/or :endpoint (s/keys :req-un [:test/request :test/response])
                                  :cli (s/keys :req-un [:test/command-line]
                                               :opt-un [:test/console-out :test/result]))))

(s/def :fault/templated-string (s/or :simple string?
                                     :variable (s/and vector?
                                                      (s/coll-of (s/or :string string?
                                                                       :variable :fault/variable)))))

(s/def :test/command-line :fault/templated-string)

(s/def :test/console-out (s/and list?
                                (s/or :file (s/cat :label #{'file}
                                                   :file-name :fault/templated-string)
                                      :plain-text (s/cat :label #{'text}
                                                         :body :fault/templated-string))))

(s/def :test/result int?)

(s/def :test/request (s/or :shorthand (s/and list?
                                             (s/cat :method :request/method
                                                    :url :request/url
                                                    :headers (s/? :endpoint/headers)
                                                    :body (s/? :endpoint/body)))
                           :map (s/keys :req-un [:request/url]
                                        :opt-un [:request/method :endpoint/headers :endpoint/body])))

(s/def :request/url (s/or :simple string?
                          :variable (s/and vector?
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

(s/def :test/response (s/or :status-only :response/status
                            :map (s/keys :opt-un [:response/status :endpoint/headers :endpoint/body])
                            :shorthand (s/and vector?
                                              (s/cat :status :response/status
                                                     :headers (s/? :endpoint/headers)
                                                     :body (s/? :endpoint/body)))))

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
