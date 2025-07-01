(ns ez-form.validation-test
  (:require [clojure.spec.alpha :as spec]
            [expectations.clojure.test :refer :all]
            [ez-form.validation :as sut]
            [ez-form.validation.validation-malli :as sutm]
            [malli.core :as m]
            [malli.util :as mu]))


(spec/def ::int int?)
(spec/def ::int>100 #(and (number? %)

                          (> % 100)))

(defexpect validation-spec-test
  (let [error-msg    "Must be an integer"
        number-field {:validation [{:spec      ::int
                                    :error-msg error-msg}]}]
    (expect []
            (sut/validate number-field {:field/value 1}))
    (expect [error-msg]
            (sut/validate number-field {:field/value "asdf"}))))

(defexpect external-validation-spec-test
  (let [error-msg           "Expected number must be 1"
        external-validation (fn [_field {:keys [field/value db]}]
                              (= (:expected-number @db)
                                 value))
        db                  (atom {:expected-number 1})
        number-field        {:validation [{:external  external-validation
                                           :error-msg error-msg}]}]
    (expect []
            (sut/validate number-field {:field/value 1
                                        :db          db}))
    (expect [error-msg]
            (sut/validate number-field {:field/value "asdf"
                                        :db          db}))))


(defexpect validation-spec-multi-test
  (let [error-msg1   "Must be an integer"
        error-msg2   "Must be higher than a 100"
        number-field {:validation [{:spec      ::int
                                    :error-msg error-msg1}
                                   {:spec      ::int>100
                                    :error-msg error-msg2}]}]
    (expect [error-msg2]
            (sut/validate number-field {:field/value 1}))
    (expect [error-msg1 error-msg2]
            (sut/validate number-field {:field/value "asdf"}))))

(defexpect validation-malli-test
  (let [error-msg    "Must be an integer"
        spec         :int
        number-field {:validation [{:spec      spec
                                    :error-msg error-msg}]}]
    (expect []
            (sutm/validate number-field {:field/value 1}))
    (expect [error-msg]
            (sutm/validate number-field "asdf"))))

(defexpect external-validation-malli-test
  (let [error-msg           "Expected number must be 1"
        external-validation (fn [_field {:keys [field/value db]}]
                              (= (:expected-number @db)
                                 value))
        db                  (atom {:expected-number 1})
        number-field        {:validation [{:external  external-validation
                                           :error-msg error-msg}]}]
    (expect []
            (sutm/validate number-field {:field/value 1
                                         :db          db}))
    (expect [error-msg]
            (sutm/validate number-field {:field/value "asdf"
                                         :db          db}))))

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
            (sutm/validate number-field {:field/value 1}))
    (expect [error-msg1 error-msg2]
            (sutm/validate number-field {:field/value "asdf"}))))

(defexpect validation-malli-complex-test
  (let [error-msg-path       "Path must be a string"
        error-msg-permission "Permission must be a combination of read, write and execute"
        Path                 :string
        Permission           [:enum "write" "read" "execute"]
        Permissions          [:set Permission]
        PathPermission       [:map
                              [:path Path]
                              [:permissions Permissions]]
        PathPermissions      [:sequential PathPermission]
        complex-fn           (fn [_field {:keys [field/value PathPermissions] :as _ctx}]
                               (when-not (m/validate PathPermissions value)
                                 (mu/explain-data PathPermissions value)))
        validation-map       {:complex              complex-fn
                              :error-msg-path       error-msg-path
                              :error-msg-permission error-msg-permission
                              :PathPermissions      PathPermissions}
        path-field           {:validation [validation-map]}]
    (expect []
            (sutm/validate path-field {:field/value []})
            "validation of empty field gives no errors back")
    (expect []
            (sutm/validate path-field {:field/value [{:path        "/foo"
                                                      :permissions #{"write" "read"}}]})
            "validation of valid field gives no errors back")
    (expect [{:validation-map validation-map
              :result         {:errors
                               [{:in    [0 :path],
                                 :path  [0 :path],
                                 :schema
                                 [:map
                                  [:path :string]
                                  [:permissions [:set [:enum "write" "read" "execute"]]]],
                                 :type  :malli.core/missing-key,
                                 :value nil}
                                {:in    [0 :permissions],
                                 :path  [0 :permissions],
                                 :schema
                                 [:map
                                  [:path :string]
                                  [:permissions [:set [:enum "write" "read" "execute"]]]],
                                 :type  :malli.core/missing-key,
                                 :value nil}],
                               :schema
                               [:sequential
                                [:map
                                 [:path :string]
                                 [:permissions [:set [:enum "write" "read" "execute"]]]]],
                               :value [{}]}}]
            (sutm/validate path-field {:field/value [{}]})
            "validation of invalid fields gives one error back")))
