(ns faultline.grammar.file
  (:require [clojure.spec.alpha :as s]
            [faultline.grammar.common :refer :all]))

(s/def :fault/file (s/and list?
                          (s/cat :label '#{file/xml file/json file/text file/yaml file/csv file/properties}
                                 :file-name :fault/templated-string)))

(s/def :file/xml (s/and list?
                        (s/cat :label '#{file/xml}
                               :file-name :fault/templated-string)))

(s/def :file/json (s/and list?
                         (s/cat :label '#{file/json}
                                :file-name :fault/templated-string)))

(s/def :file/text (s/and list?
                         (s/cat :label '#{file/text}
                                :file-name :fault/templated-string)))

(s/def :file/yaml (s/and list?
                         (s/cat :label '#{file/yaml}
                                :file-name :fault/templated-string)))

(s/def :file/csv (s/and list?
                        (s/cat :label '#{file/csv}
                               :file-name :fault/templated-string)))

(s/def :file/properties (s/and list?
                               (s/cat :label '#{file/properties}
                                      :file-name :fault/templated-string)))
