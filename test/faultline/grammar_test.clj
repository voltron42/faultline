(ns faultline.grammar-test
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [faultline.commmon.validation :refer :all]
            [faultline.grammar.endpoint :refer :all]
            [faultline.grammar.db :refer :all]
            [faultline.grammar.core :refer :all]
            ))

(deftest validate-request
  (is (= nil (s/explain-data
               :test/request
               '(request GET :request-url {}))))

  (is (= nil (s/explain-data
               :test/request
               '(request POST :permissions-url {} (json {"permissions" "restricted"
                                                      "pbm" "wxyz"
                                                      "ehr" "56789"})))))

  (is (= nil (s/explain-data
               :test/request
               '(request POST :permissions-url {} (file/json :path :suite "abc_123_restricted_req.json")))))

  (is (= nil (s/explain-data
               :test/request
               '(request POST :permissions-url {} (json {"permissions" {"COP" {"DS" "restricted"}}
                                                      "pbm" "ESI3"
                                                      "ehr" "PracticeFusion"})))))
  )

(deftest validate-response
  (is (= nil (s/explain-data
               :test/response
               '(response 200 {}))))
  (is (= nil (s/explain-data
               :test/response
               '(response 200 {} "restricted"))))
  (is (= nil (s/explain-data
               :test/response
               '(response 200 {} (file/json :path :suite "abc_123_restricted_resp.json")))))
  (is (= nil (s/explain-data
               :test/response
               '(response 200 {} (json {"permissions" {"COP" {"DS" "restricted"}}
                                     "pbm" "ESI3"
                                     "ehr" "PracticeFusion"})))))
  )

(deftest validate-test
  (is (= nil (s/explain-data
               :fault/test
               '(test
                  test-1
                  (request GET :request-url {})
                  (response 200 {}))))))

(deftest validate-suite
  (is (= nil (s/explain-data
               :fault/suite
               '(suite
                  suite-1
                  (test
                    test-1
                    (request GET :request-url {})
                    (response 200 {})))))))

(deftest validate-regression
  (is (= nil (s/explain-data
               :fault/testfile
               '[(suite
                   suite-1
                   (test
                     test-1
                     (request GET :request-url {})
                     (response 200 {})))]))))

(deftest validate-properties
  (is (= nil (s/explain-data
               :fault/properties
               '(properties (file/properties :path :env "test.properties"))))))

(deftest validate-tags
  (is (= nil (s/explain-data
               :fault/tags
               '(tags formulary-permissions-service)))))

(deftest validate-db-config
  (is (= nil (s/explain-data
               :db/config
               '(db/config :permissions-db-config)))))

(deftest validate-db-tables
  (is (= nil (s/explain-data
               :db/tables
               '(db/tables permissions)))))

(deftest validate-db-table-name
  (is (= nil (s/explain-data
               :db/table-name
               'permissions))))

(deftest validate-db-load
  (is (= nil (s/explain-data
               :db/load
               '(db/load permissions (file/csv :path :suite "testData1.csv")))))
  (is (= nil (s/explain-data
               :db/load
               '(db/load permissions (file/csv :path :suite "testData1ALT.csv")))))
  )

(deftest validate-db-clean
  (is (= nil (s/explain-data
               :db/clean
               '(db/clean permissions (file/csv :path :suite "testData1.csv")))))
  (is (= nil (s/explain-data
               :db/clean
               '(db/clean permissions (file/csv :path :suite "testData1ALT.csv")))))
  )

(deftest validate-table
  (is (= nil (s/explain-data
               :data/table
               '(table
                  [pbm_uid ehr_uid type sub_type list_id]
                  ["ESI3" "PracticeFusion" "COP" "DS" "*"])))))

(deftest validate-validate
  (is (= nil (s/explain-data
               :table/validate
               '(validate
                  [pbm_uid ehr_uid type sub_type list_id]
                  ["ESI3" "PracticeFusion" "COP" "DS" "*"])))))

(deftest validate-db-created
  (is (= nil (s/explain-data
               :db/created
               '(db/created permissions (file/csv :path :suite "FPS_COP_Standard.csv")))))
  (is (= nil (s/explain-data
               :db/created
               '(db/created permissions (EMPTY))
               )))
  (is (= nil (s/explain-data
               :db/created
               '(db/created permissions (table
                                          [pbm_uid ehr_uid type sub_type list_id]
                                          ["ESI3" "PracticeFusion" "COP" "DS" "*"]))
               )))
  (is (= nil (s/explain-data
               :db/created
               '(db/created permissions (validate
                                          [pbm_uid ehr_uid type sub_type list_id deactivated_date]
                                          ["ESI3" "PracticeFusion" "COP" "DS" "*" (is-not-null)])))))
  )

(deftest validate-db-delete
  (is (= nil (s/explain-data
               :db/deleted
               '(db/deleted permissions (file/csv :path :suite "FPS_COP_Standard.csv")))))
  (is (= nil (s/explain-data
               :db/deleted
               '(db/deleted permissions (EMPTY))
               )))
  (is (= nil (s/explain-data
               :db/deleted
               '(db/deleted permissions (table
                                          [pbm_uid ehr_uid type sub_type list_id]
                                          ["ESI3" "PracticeFusion" "COP" "DS" "*"]))
               )))
  (is (= nil (s/explain-data
               :db/deleted
               '(db/deleted permissions (validate
                                          [pbm_uid ehr_uid type sub_type list_id deactivated_date]
                                          ["ESI3" "PracticeFusion" "COP" "DS" "*" (is-not-null)])))))
  )

(deftest validate-file-csv
  (is (= nil (s/explain-data
               :file/csv
               '(file/csv :path :suite "FPS_COP_Standard_before.csv")))))

(deftest validate-db-updated
  (is (= nil (s/explain-data
               :db/updated
               '(db/updated permissions
                            (file/csv :path :suite "FPS_COP_Standard_before.csv")
                            (file/csv :path :suite "FPS_COP_Standard_after.csv")))))
  (is (= nil (s/explain-data
               :db/updated
               '(db/updated permissions (EMPTY)))))
  (is (= nil (s/explain-data
               :db/updated
               '(db/updated permissions
                            (table
                              [pbm_uid ehr_uid type sub_type list_id]
                              ["ESI3" "PracticeFusion" "COP" "DS" "*"])
                            (table
                              [pbm_uid ehr_uid type sub_type list_id]
                              ["ESI3" "PracticeFusion" "COP" "DS" "*"])))))
  (is (= nil (s/explain-data
               :db/updated
               '(db/updated permissions
                            (validate
                              [pbm_uid ehr_uid type sub_type list_id deactivated_date]
                              ["ESI3" "PracticeFusion" "COP" "DS" "*" (is-not-null)])
                            (validate
                              [pbm_uid ehr_uid type sub_type list_id deactivated_date]
                              ["ESI3" "PracticeFusion" "COP" "DS" "*" null])))))
  (is (= nil (s/explain-data
               :db/updated
               '(db/updated permissions
                            (validate
                              [pbm_uid ehr_uid type sub_type list_id deactivated_date]
                              ["ESI3" "PracticeFusion" "COP" "DS" "*" (is-not-null)])
                            (table
                              [pbm_uid ehr_uid type sub_type list_id deactivated_date]
                              ["ESI3" "PracticeFusion" "COP" "DS" "*" null])))))
  )

