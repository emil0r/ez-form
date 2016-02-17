(ns ez-form.paragraph
  (:require [ez-form.field :as ez.field :refer [get-first option error-div get-opts]]))


(defmulti paragraph (fn [field form-options] (:type field)))

(defmethod paragraph :checkbox [field form-options]
  (list
   [:p (ez.field/label field)]
   [:p
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (if-let [help (ez.field/help field)]
      [:div.help help])]))

(defmethod paragraph :radio [field form-options]
  (list
   [:p (ez.field/label field)]
   [:p
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (if-let [help (ez.field/help field)]
      [:div.help help])]))

(defmethod paragraph :textarea [field form-options]
  (list
   [:p (ez.field/label field)]
   [:p
    (ez.field/errors field)
    (ez.field/field field form-options)
    (if-let [help (ez.field/help field)]
      [:div.help help])]))

(defmethod paragraph :dropdown [field form-options]
  (list
   [:p (ez.field/label field)]
   [:p
    (ez.field/errors field)
    (ez.field/field field form-options)
    (if-let [help (ez.field/help field)]
      [:div.help help])]))

(defmethod paragraph :default [field form-options]
  "Any input that is like text will work with this one"
  (list
   [:p (ez.field/label field)]
   [:p
    (ez.field/errors field)
    (ez.field/field field form-options)
    (if-let [help (ez.field/help field)]
      [:div.help help])]))
