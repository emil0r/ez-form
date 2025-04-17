(ns ez-form.validation-test
  (:require [clojure.spec.alpha :as spec]
            [expectations.clojure.test :refer :all]
            [ez-form.validation :as sut]
            [ez-form.validation.validation-malli :as sutm]))


(spec/def ::int int?)
(spec/def ::int>100 #(and (number? %)
                          (> % 100)))

(defexpect validation-spec-test
  (let [error-msg    "Must be an integer"
        number-field {:validation [{:spec      ::int
                                    :error-msg error-msg}]}]
    (expect []
            (-> (sut/validate number-field 1)
                :errors))
    (expect [error-msg]
            (-> (sut/validate number-field "asdf")
                :errors))))

(defexpect validation-spec-multi-test
  (let [error-msg1    "Must be an integer"
        error-msg2    "Must be higher than a 100"
        number-field {:validation [{:spec      ::int
                                    :error-msg error-msg1}
                                   {:spec      ::int>100
                                    :error-msg error-msg2}]}]
    (expect [error-msg2]
            (-> (sut/validate number-field 1)
                :errors))
    (expect [error-msg1 error-msg2]
            (-> (sut/validate number-field "asdf")
                :errors))))

(defexpect validation-malli-test
  (let [error-msg    "Must be an integer"
        spec         :int
        number-field {:validation [{:spec      spec
                                    :error-msg error-msg}]}]
    (expect []
            (-> (sutm/validate number-field 1)
                :errors))
    (expect [error-msg]
            (-> (sutm/validate number-field "asdf")
                :errors))))

(defexpect validation-malli-multi-test
  (let [error-msg1   "Must be an integer"
        error-msg2   "Must be higher than a 100"
        spec1        :int
        spec2        [:fn #(> % 100)]
        number-field {:validation [{:spec      spec1
                                    :error-msg error-msg1}
                                   {:spec      spec2
                                    :error-msg error-msg2}]}]
    (expect [error-msg2]
            (-> (sutm/validate number-field 1)
                :errors))
    (expect [error-msg1 error-msg2]
            (-> (sutm/validate number-field "asdf")
                :errors))))
