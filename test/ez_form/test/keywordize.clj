(ns ez-form.test.keywordize
  (:require [clojure.test :refer [deftest is testing]]
            [ez-form.keywordize :as keywordize]))


(deftest keywords
  (testing "Strings as valid keywords"
    (is (every? string? (map keywordize/keyword-syntax? ["__ez-form.form-name"
                                                         "foo"
                                                         "foo.bar"
                                                         "foo.bar/baz"
                                                         "foo/baz"]))
        "Is not valid")))
