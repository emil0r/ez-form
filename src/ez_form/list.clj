(ns ez-form.list
  (:require [ez-form.field :as ez.field :refer [get-first option error-div get-opts]]))


(defmulti li (fn [field form-options] (:type field)))

(defmethod li :checkbox [field form-options]
  (let [text (:text field)]
    [:li
     (ez.field/label field)
     (ez.field/errors field)
     (ez.field/field field form-options)
     text]))

(defmethod li :radio [field form-options]
  (let [text (:text field)]
    [:li
     (ez.field/label field)
     (ez.field/errors field)
     (ez.field/field field form-options)
     text]))

(defmethod li :textarea [field form-options]
  [:li
   (ez.field/label field)
   (ez.field/errors field)
   (ez.field/field field form-options)])

(defmethod li :dropdown [field form-options]
  [:li
   (ez.field/label field)
   (ez.field/errors field)
   (ez.field/field field form-options)])

(defmethod li :default [field form-options]
  "Any input that is like text will work with this one"
  [:li
   (ez.field/label field)
   (ez.field/errors field)
   (ez.field/field field form-options)])
