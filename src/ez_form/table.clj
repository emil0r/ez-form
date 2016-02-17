(ns ez-form.table
  (:require [ez-form.field :as ez.field :refer [get-first option error-div get-opts]]))

(defmulti row (fn [field form-options] (:type field)))

(defmethod row :checkbox [field form-options]
  [:tr
   [:th (ez.field/label field)]
   [:td
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (if-let [help (ez.field/help field)]
      [:div.help help])]])

(defmethod row :radio [field form-options]
  [:tr
   [:th (ez.field/label field)]
   [:td
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (if-let [help (ez.field/help field)]
      [:div.help help])]])

(defmethod row :textarea [field form-options]
  [:tr
   [:th (ez.field/label field)]
   [:td
    (ez.field/errors field)
    (ez.field/field field form-options)
    (if-let [help (ez.field/help field)]
      [:div.help help])]])

(defmethod row :dropdown [field form-options]
  [:tr
   [:th (ez.field/label field)]
   [:td
    (ez.field/errors field)
    (ez.field/field field form-options)
    (if-let [help (ez.field/help field)]
      [:div.help help])]])

(defmethod row :default [field form-options]
  "Any input that is like text will work with this one"
  [:tr
   [:th (ez.field/label field)]
   [:td
    (ez.field/errors field)
    (ez.field/field field form-options)
    (if-let [help (ez.field/help field)]
      [:div.help help])]])
