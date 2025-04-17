(ns ez-form.field-test
  (:require [ez-form.field :as sut]
            [expectations.clojure.test :refer :all]))

(defexpect input-field-test
  (expect
   [:input {:type        :text
            :name        :test
            :value       "value"
            :placeholder "placeholder"}]
   (sut/input-field {:attributes {:name        :test
                                  :value       "value"
                                  :placeholder "placeholder"}})
   "text input")
  (expect
   [:input {:type        :email
            :name        :email
            :value       "john@doe.com"
            :placeholder "user@example.com"}]
   (sut/input-field {:type       :email
                     :attributes {:name        :email
                                  :value       "john@doe.com"
                                  :placeholder "user@example.com"}})
   "email input")
  (expect
   [:select {:name :select}
    (list [:option {:value "" :selected false} "..."]
          [:option {:value "option-1" :selected true} "Option 1"]
          [:option {:value "option-2" :selected false} "Option 2"])]
   (sut/select-field {:type       :select
                      :attributes {:name  :select
                                   :value "option-1"}
                      :options    [["" "..."]
                                   ["option-1" "Option 1"]
                                   ["option-2" "Option 2"]]})
   "select"))

(defexpect textarea-field-test
  (expect
   [:textarea {:name :textarea :rows 3 :cols 4} "value"]
   (sut/textarea-field {:attributes {:name :textarea
                                     :rows 3
                                     :cols 4
                                     :value "value"}})))
