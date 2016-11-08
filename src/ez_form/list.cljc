(ns ez-form.list
  (:require [ez-form.common :refer [get-first]]
            [ez-form.decorate :refer [add-decor]]
            [ez-form.field :as ez.field :refer [get-opts
                                                option]]))


(defmulti li (fn [field form-options] (:type field)))

(defmethod li :checkbox [field form-options]
  [:li (add-decor :wrapper field)
   (ez.field/label field)
   (ez.field/errors field)
   (ez.field/field field form-options)
   (ez.field/text field)
   (ez.field/help field)])

(defmethod li :radio [field form-options]
  [:li (add-decor :wrapper field)
   (ez.field/label field)
   (ez.field/errors field)
   (ez.field/field field form-options)
   (ez.field/text field)
   (ez.field/help field)])

(defmethod li :textarea [field form-options]
  [:li (add-decor :wrapper field)
   (ez.field/label field)
   (ez.field/errors field)
   (ez.field/field field form-options)
   (ez.field/text field)
   (ez.field/help field)])

(defmethod li :dropdown [field form-options]
  [:li (add-decor :wrapper field)
   (ez.field/label field)
   (ez.field/errors field)
   (ez.field/field field form-options)
   (ez.field/text field)
   (ez.field/help field)])

(defmethod li :default [field form-options]
  "Any input that is like text will work with this one"
  [:li (add-decor :wrapper field)
   (ez.field/label field)
   (ez.field/errors field)
   (ez.field/field field form-options)
   (ez.field/text field)
   (ez.field/help field)])
