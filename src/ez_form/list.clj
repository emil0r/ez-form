(ns ez-form.list
  (:require [ez-form.field :as ez.field :refer [get-first option error-div get-opts]]))


(defmulti li (fn [field form-options] (:type field)))

(defmethod li :checkbox [field form-options]
  (let [label (get-first field true :label :name)
        id (get-first field :id :name)
        text (:text field)
        errors (:errors field)]
    [:li
     [:label {:for id} label]
     (if errors
       (map error-div errors))
     (ez.field/field field form-options)
     text]))

(defmethod li :radio [field form-options]
  (let [label (get-first field true :label :name)
        id (get-first field :id :name)
        text (:text field)
        errors (:errors field)]
    [:li
     [:label {:for id} label]
     (if errors
       (map error-div errors))
     (ez.field/field field form-options)
     text]))

(defmethod li :textarea [field form-options]
  (let [label (get-first field true :label :name)
        id (get-first field :id :name)
        errors (:errors field)]
    [:li
     [:label {:for id} label]
     (if errors
       (map error-div errors))
     (ez.field/field field form-options)]))

(defmethod li :dropdown [field form-options]
  (let [label (get-first field true :label :name)
        id (get-first field :id :name)
        errors (:errors field)]
    [:li
     [:label {:for id} label]
     (if errors
       (map error-div errors))
     (ez.field/field field form-options)]))

(defmethod li :default [field form-options]
  "Any input that is like text will work with this one"
  (let [label (get-first field true :label :name)
        id (get-first field :id :name)
        errors (:errors field)]
    [:li
     [:label {:for id} label]
     (if errors
       (map error-div errors))
     (ez.field/field field form-options)]))
