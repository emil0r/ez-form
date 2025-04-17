(ns ez-form.core-test
  (:require [ez-form.core :as sut]
            [expectations.clojure.test :refer :all]
            [clojure.string :as str]))

(defexpect render-test
  (let [form {:ez-form/fields {::username {:type       :text
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
  (let [user-error1    :error.username/must-exist
        email-error1   :error.email/must-exist
        email          "john.doe@example.com"
        form           {:meta {:validation :spec}
                        :ez-form/fields
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
        processed-form (sut/post-process-form form {:username ""
                                                    :email    email})]
    (expect
     [user-error1]
     (get-in processed-form [:ez-form/fields ::username :errors]))
    (expect
     []
     (get-in processed-form [:ez-form/fields ::email :errors]))
    (expect
     {:name        :email
      :value       email
      :placeholder :ui.email/placeholder}
     (get-in processed-form [:ez-form/fields ::email :attributes]))))

(defexpect post-process-form-malli-test
  (let [user-error1    :error.username/must-exist
        email-error1   :error.email/must-exist
        email          "john.doe@example.com"
        form           {:meta {:validation :malli
                               :validation-fns {:malli 'ez-form.validation.validation-malli/validate}}
                        :ez-form/fields
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
        processed-form (sut/post-process-form form {:username ""
                                                    :email    email})]
    (expect
     [user-error1]
     (get-in processed-form [:ez-form/fields ::username :errors]))
    (expect
     []
     (get-in processed-form [:ez-form/fields ::email :errors]))
    (expect
     {:name        :email
      :value       email
      :placeholder :ui.email/placeholder}
     (get-in processed-form [:ez-form/fields ::email :attributes]))))


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
