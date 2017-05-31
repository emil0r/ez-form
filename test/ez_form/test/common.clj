(ns ez-form.test.common
  (:require [clojure.test :refer [ deftest testing is]]
            [ez-form.common :as common]))

(deftest get-field
  (testing "get-field can work with namespaced keywords"
    (is (= :foo.bar/baz (common/get-keyword :?foo.bar/baz.wrapper true)))))
