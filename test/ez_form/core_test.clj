(ns ez-form.core-test
  (:require [ez-form.core :as sut]
            [expectations.clojure.test :refer :all]
            [clojure.string :as str]))

(defexpect render-test
  (let [form {:fields {::username {:type       :text
                                   :errors     ["Error 1"
                                                "Error 2"]
                                   :attributes {:name        :username
                                                :value       "johndoe"
                                                :placeholder :ui.username/placeholder}}}}]
    (expect
     [:div
      [:input {:type        :text
               :name        :username
               :value       "johndoe"
               :placeholder :ui.username/placeholder}]]
     (sut/render form [:div
                       [::username]])
     "Field is rendered")
    (expect
     [:div
      [:input {:type        :text
               :name        :username
               :value       "johndoe"
               :placeholder :ui.username/placeholder}]
      ;; this looks horrible, but it allows for arbitrary hiccup
      ;; inside errors
      '(([:div.error "Error 1"])
        ([:div.error "Error 2"]))]
     (sut/render form [:div
                       [::username]
                       [::username.errors
                        [:div.error ::username.error]]])
     "Field is rendered with errors")))

(defexpect post-process-form-test
  (let [email          "john.doe@example.com"
        username       "john.doe"
        form           {:meta {:validation :spec
                               :form-name  "test"
                               :field-data {:username username}}
                        :fields
                        {::username {:type       :text
                                     :attributes {:placeholder :ui.username/placeholder}}
                         ::email    {:type       :email
                                     :attributes {:placeholder :ui.email/placeholder}}}}
        processed-form (sut/post-process-form form {:email               email
                                                    :__ez-form.form-name "test"})]
    (expect
     {:name        :username
      :value       username
      :placeholder :ui.username/placeholder}
     (get-in processed-form [:fields ::username :attributes])
     "username has value given by [:meta :field-data :username]")
    (expect
     {:name        :email
      :value       email
      :placeholder :ui.email/placeholder}
     (get-in processed-form [:fields ::email :attributes])
     "email has all html attributes")))

(defexpect post-process-form-spec-test
  (let [user-error1    :error.username/must-exist
        email-error1   :error.email/must-exist
        email          "john.doe@example.com"
        form           {:meta {:validation :spec
                               :form-name  "test"}
                        :fields
                        {::username {:type       :text
                                     :validation [{:spec      #(not (str/blank? %))
                                                   :error-msg user-error1}]
                                     :attributes {:name        :username
                                                  :placeholder :ui.username/placeholder}}
                         ::email    {:type       :email
                                     :validation [{:spec      string?
                                                   :error-msg email-error1}]
                                     :attributes {:name        :email
                                                  :placeholder :ui.email/placeholder}}}}
        processed-form (sut/post-process-form form {:username            ""
                                                    :email               email
                                                    :__ez-form.form-name "test"})]
    (expect
     [user-error1]
     (get-in processed-form [:fields ::username :errors])
     "username has one error")
    (expect
     []
     (get-in processed-form [:fields ::email :errors])
     "email has no errors")))

(defexpect post-process-form-malli-test
  (let [user-error1    :error.username/must-exist
        email-error1   :error.email/must-exist
        email          "john.doe@example.com"
        form           {:meta {:validation     :malli
                               :form-name      "test"
                               :validation-fns {:malli 'ez-form.validation.validation-malli/validate}}
                        :fields
                        {::username {:type       :text
                                     :validation [{:spec      [:fn #(not (str/blank? %))]
                                                   :error-msg user-error1}]
                                     :attributes {:name        :username
                                                  :placeholder :ui.username/placeholder}}
                         ::email    {:type       :email
                                     :validation [{:spec      :string
                                                   :error-msg email-error1}]
                                     :attributes {:name        :email
                                                  :placeholder :ui.email/placeholder}}}}
        processed-form (sut/post-process-form form {:username            ""
                                                    :email               email
                                                    :__ez-form.form-name "test"})]
    (expect
     [user-error1]
     (get-in processed-form [:fields ::username :errors]))
    (expect
     []
     (get-in processed-form [:fields ::email :errors]))))

(defexpect defform-test
  (sut/defform testform
    {}
    [{:name ::username
      :type :text}
     {:name ::email
      :type :email}])
  (let [form (testform {:username "foobar"}
                       {:__ez-form.form-name "testform"
                        :email               "john.doe@example.com"})]
    (expect
     "testform"
     (get-in form [:meta :form-name])
     "form-name is set based on the name of the form in defform")
    (expect
     "foobar"
     (get-in form [:fields ::username :value])
     "username's value is set on the data being sent in")
    (expect
     "john.doe@example.com"
     (get-in form [:fields ::email :value])
     "email's value is set on the params being sent in")
    (expect
     [::username ::email]
     (get-in form [:meta :field-order])
     "field order is set according to the order in which fields are sent in")
    (expect
     true
     (sut/valid? form)
     "Form is valid")))



;; (def form-test1
;;   [{:type               :text
;;     :name               "username"
;;     :value              "my initial value"
;;     :placeholder        ""
;;     :ez-form/validation #()}])

;; (def form-test2
;;   {::username {:type       :text
;;                :css        ["foo" "bar" "baz"]
;;                :attributes {:name        "username"
;;                             :value       "my initial value"
;;                             :placeholder ""}
;;                :validation #()}})

;; [:div {:class [::username :css]}
;;  [::username]
;;  [::username :errors]]
