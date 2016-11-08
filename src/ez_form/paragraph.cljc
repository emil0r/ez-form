(ns ez-form.paragraph
  (:require [ez-form.common :refer [get-first]]
            [ez-form.decorate :refer [add-decor]]
            [ez-form.field :as ez.field :refer [get-opts
                                                option]]))


(defmulti paragraph (fn [field form-options] (:type field)))

(defmethod paragraph :checkbox [field form-options]
  (list

   [:p (ez.field/label field)]
   [:p (add-decor :wrapper field)
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (ez.field/help field)]))

(defmethod paragraph :radio [field form-options]
  (list
   [:p (ez.field/label field)]
   [:p (add-decor :wrapper field)
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (ez.field/help field)]))

(defmethod paragraph :textarea [field form-options]
  (list
   [:p (ez.field/label field)]
   [:p (add-decor :wrapper field)
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (ez.field/help field)
    (if-let [help (ez.field/help field)]
      [:div.help help])]))

(defmethod paragraph :dropdown [field form-options]
  (list
   [:p (ez.field/label field)]
   [:p (add-decor :wrapper field)
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (ez.field/help field)]))

(defmethod paragraph :default [field form-options]
  "Any input that is like text will work with this one"
  (list
   [:p (ez.field/label field)]
   [:p (add-decor :wrapper field)
    (ez.field/errors field)
    (ez.field/field field form-options)
    (ez.field/text field)
    (ez.field/help field)]))
