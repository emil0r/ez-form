(ns ez-form.field-test
  (:require [ez-form.field :as sut]
            [expectations.clojure.test :refer :all]))

(defexpect input-field-test
  (expect
   [:input {:type        :text
            :name        :test
            :value       "value"
            :placeholder "placeholder"}]
   (sut/input-field {:type       :text
                     :attributes {:name        :test
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
   "select")
  (expect
   (list [:label
          [:input {:type        :radio
                   :name        :test
                   :value       "A"
                   :checked     false
                   :placeholder "placeholder"}]
          "This is A"]
         [:label
          [:input {:type        :radio
                   :name        :test
                   :value       "B"
                   :checked     true
                   :placeholder "placeholder"}]
          "B"]
         [:label
          [:input {:type        :radio
                   :name        :test
                   :value       "C"
                   :checked     false
                   :placeholder "placeholder"}]
          "C"])
   (sut/input-radio-field {:type       :radio
                           :value      "B"
                           :options    [["A" "This is A"]
                                        "B"
                                        "C"]
                           :attributes {:name        :test
                                        :value       "B"
                                        :placeholder "placeholder"}})
   "radio input")
  (expect
   (list [:label
          [:input {:type        :checkbox
                   :name        :test
                   :value       "A"
                   :checked     true
                   :placeholder "placeholder"}]
          "This is A"]
         [:label
          [:input {:type        :checkbox
                   :name        :test
                   :value       "B"
                   :checked     true
                   :placeholder "placeholder"}]
          "B"]
         [:label
          [:input {:type        :checkbox
                   :name        :test
                   :value       "C"
                   :checked     false
                   :placeholder "placeholder"}]
          "C"])
   (sut/input-checkbox-field {:type       :checkbox
                              :value      ["A" "B"]
                              :options    [["A" "This is A"]
                                           "B"
                                           "C"]
                              :attributes {:name        :test
                                           :value       ["A" "B"]
                                           :placeholder "placeholder"}})
   "checkbox input")
  (expect
   (list [:label
          [:input {:type        :checkbox
                   :name        :test
                   :value       "A"
                   :checked     true
                   :placeholder "placeholder"}]
          "This is A"]
         [:label
          [:input {:type        :checkbox
                   :name        :test
                   :value       "B"
                   :checked     false
                   :placeholder "placeholder"}]
          "B"]
         [:label
          [:input {:type        :checkbox
                   :name        :test
                   :value       "C"
                   :checked     false
                   :placeholder "placeholder"}]
          "C"])
   (sut/input-checkbox-field {:type       :checkbox
                              :value      "A"
                              :options    [["A" "This is A"]
                                           "B"
                                           "C"]
                              :attributes {:name        :test
                                           :value       "A"
                                           :placeholder "placeholder"}})
   "checkbox input with single value")
  ;; TODO: Make this test pass
  #_(expect
     (list [:label
            [:input {:type        :checkbox
                     :name        :test
                     :value       "A"
                     :checked     true
                     :placeholder "placeholder"}]
            "This is A"]
           [:label
            [:input {:type        :checkbox
                     :name        :test
                     :value       "B"
                     :checked     false
                     :placeholder "placeholder"}]
            "B"]
           [:label
            [:input {:type        :checkbox
                     :name        :test
                     :value       "C"
                     :checked     false
                     :placeholder "placeholder"}]
            "C"])
     (sut/input-checkbox-field {:type       :checkbox
                                :value      ["A"]
                                :options    [["A" "This is A"]
                                             ["A" "B"]
                                             ["A" "C"]]
                                :attributes {:name        :test
                                             :value       "A"
                                             :placeholder "placeholder"}})
     "checkbox input edge case with multiple options that has the same value, but not all are picked")
  (expect
   [:input {:type        :text
            :name        :test
            :value       "value"
            :placeholder "placeholder"}]
   (sut/render {:type       :text
                :attributes {:name        :test
                             :value       "value"
                             :placeholder "placeholder"}}
               sut/fields)
   "render function")
  (expect
   "I am missing the field :test"
   (sut/render {:type       :foo
                :attributes {:name        :test
                             :value       "value"
                             :placeholder "placeholder"}}
               sut/fields)
   "render function - field type does not exist"))

(defexpect textarea-field-test
  (expect
   [:textarea {:name :textarea :rows 3 :cols 4} "value"]
   (sut/textarea-field {:attributes {:name  :textarea
                                     :rows  3
                                     :cols  4
                                     :value "value"}})))
