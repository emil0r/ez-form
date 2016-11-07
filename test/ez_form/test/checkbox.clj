(ns ez-form.test.checkbox
  (:require [ez-form.core :as ez-form :refer [defform]]
            [ez-form.flow :as flow]
            [vlad.core :as vlad]))


(defn t [arg]
  arg)

(defform testform
  {}
  [{:type :checkbox
    :name :single
    :value "single"
    :label "my checkbox"}
   {:type :checkbox
    :name :multiple
    :label "my checkbox"
    :options ["one"
              "two"]}])


(ez-form/as-flow
 [:div :single.field :multiple.field]
 (testform nil {:single "asdf" :multiple "one"}))
