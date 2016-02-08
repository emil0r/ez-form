(ns ez-form.paragraph
  (:require [ez-form.field :as ez.field :refer [get-first option error-div get-opts]]))


(defmulti paragraph (fn [field form-options] (:type field)))

(defmethod paragraph :checkbox [field form-options]
  (let [label (get-first field true :label :name)
        id (get-first field :id :name)
        text (:text field)
        errors (:errors field)]
    (list
     [:p [:label {:for id} label]]
     [:p
      (if errors
        (map error-div errors))
      (ez.field/field field form-options)
      text])))

(defmethod paragraph :radio [field form-options]
  (let [label (get-first field true :label :name)
        id (get-first field :id :name)
        text (:text field)
        errors (:errors field)]
    (list
     [:p [:label {:for id} label]]
     [:p
      (if errors
        (map error-div errors))
      (ez.field/field field form-options)
      text])))

(defmethod paragraph :textarea [field form-options]
  (let [label (get-first field true :label :name)
        id (get-first field :id :name)
        errors (:errors field)]
    (list
     [:p [:label {:for id} label]]
     [:p
      (if errors
        (map error-div errors))
      (ez.field/field field form-options)])))

(defmethod paragraph :dropdown [field form-options]
  (let [label (get-first field true :label :name)
        id (get-first field :id :name)
        errors (:errors field)]
    (list
     [:p [:label {:for id} label]]
     [:p
      (if errors
        (map error-div errors))
      (ez.field/field field form-options)])))

(defmethod paragraph :default [field form-options]
  "Any input that is like text will work with this one"
  (let [label (get-first field true :label :name)
        id (get-first field :id :name)
        errors (:errors field)]
    (list
     [:p [:label {:for id} label]]
     [:p
      (if errors
        (map error-div errors))
      (ez.field/field field form-options)])))
