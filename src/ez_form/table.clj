(ns ez-form.table
  (:require [ez-form.decorate :refer [add-decor]]
            [ez-form.field :as ez.field :refer [error-div
                                                get-first
                                                get-opts
                                                option]]))

(defmulti row (fn [field form-options] (:type field)))

(defmethod row :checkbox [field form-options]
  [:tr (add-decor :wrapper field)
   [:th (ez.field/label field)]
   [:td
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (ez.field/help field)]])

(defmethod row :radio [field form-options]
  [:tr (add-decor :wrapper field)
   [:th (ez.field/label field)]
   [:td
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (ez.field/help field)]])

(defmethod row :textarea [field form-options]
  [:tr (add-decor :wrapper field)
   [:th (ez.field/label field)]
   [:td
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (ez.field/help field)]])

(defmethod row :dropdown [field form-options]
  [:tr (add-decor :wrapper field)
   [:th (ez.field/label field)]
   [:td
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (ez.field/help field)]])

(defmethod row :default [field form-options]
  "Any input that is like text will work with this one"
  [:tr (add-decor :wrapper field)
   [:th (ez.field/label field)]
   [:td
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (ez.field/help field)]])
