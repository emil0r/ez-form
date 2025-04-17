(ns ez-form.core-test
  (:require [ez-form.core :as sut]
            [expectations.clojure.test :refer :all]))

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
