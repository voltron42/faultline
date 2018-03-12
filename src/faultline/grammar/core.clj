(ns faultline.grammar.core
  (:require [clojure.spec.alpha :as s]
            [faultline.grammar.common :as c]
            [faultline.grammar.data :refer :all]
            [faultline.grammar.file :refer :all]
            [faultline.grammar.db :refer :all]
            [faultline.grammar.endpoint :refer :all]
            [faultline.commmon.validation :as v]))

(s/def :suite/name (s/and symbol (v/named-as c/valid-name)))

(s/def :test/name (s/and symbol (v/named-as c/valid-name)))

(s/def :fault/comment string?)

(s/def :config/value
  (s/or :complex :data/structured
        :variable :fault/variable
        :text string?
        :number number?
        :boolean boolean?))

(s/def :fault/properties
  (s/and list?
         (s/cat :label #{'properties}
                :properties (s/or :file :file/properties
                                  :content :data/params))))


(s/def :fault/assign (s/and list?
                            (s/cat :label #{'assign}
                                   :variable :fault/variable
                                   :value :config/value)))

(s/def :fault/tags (s/and list?
                          (s/cat :label #{'tags}
                                 :tags (s/+ :fault/tag-name))))

(s/def :fault/assign-or-comment (s/or :assign :fault/assign
                                      :comment :fault/comment
                                      :tags :fault/tags))

(s/def :fault/import (s/cat :properties (s/* (s/or :assign-or-comment :fault/assign-or-comment
                                                   :props :fault/properties))
                            :db-config (s/? :db/config)
                            :db-tables (s/* (s/or :assign-or-comment :fault/assign-or-comment
                                                  :tables :db/tables))))

(s/def :fault/setup (s/or :db-load :db/load
                          :assign-or-comment :fault/assign-or-comment))

(s/def :fault/precondition (s/or :comment :fault/comment
                                 :db-state :db/state))

(s/def :fault/postcondition (s/or :db-created :db/created
                                  :db-deleted :db/deleted
                                  :db-updated :db/updated
                                  :db-state :db/state
                                  :comment :fault/comment))

(s/def :fault/teardown (s/or :db-clean :db/clean
                             :comment :fault/comment))

(s/def :fault/test (s/and list?
                          (s/cat :label #{'test}
                                 :test-name :test/name
                                 :setup (s/* :fault/setup)
                                 :preconditions (s/* :fault/precondition)
                                 :request :test/request
                                 :response :test/response
                                 :postconditions (s/* :fault/postcondition)
                                 :teardown (s/* :fault/teardown))))

(s/def :fault/suite (s/and list?
                           (s/cat :label #{'suite}
                                  :test-name :suite/name
                                  :setup (s/* :fault/setup)
                                  :tests (s/+ :fault/test)
                                  :teardown (s/* :fault/teardown))))

(s/def :fault/testfile (s/cat :imports (s/? :fault/import)
                              :setup (s/* :fault/setup)
                              :suites (s/+ :fault/suite)
                              :teardown (s/* :fault/teardown)))
