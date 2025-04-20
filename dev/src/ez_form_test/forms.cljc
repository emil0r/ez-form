(ns ez-form-test.forms
  (:require [clojure.string :as str]
            #?(:clj  [ez-form.core :refer [defform]]
               :cljs [ez-form.core]))
  #?(:cljs (:require-macros [ez-form.core :refer [defform]])))


(defn sl-input-color-picker [{:keys [type attributes]}]
  (let [type* (name type)]
    [type (merge attributes
                 {:type (subs type* 3 (count type*))})]))

(defn sl-input [{:keys [type attributes]}]
  (let [type* (name type)]
    [:sl-input (merge attributes
                      {:type (subs type* 9 (count type*))})]))

(defform replicant-form
  {:extra-fields {:sl-color-picker sl-input-color-picker
                  :sl-input-email  sl-input
                  :sl-input-number sl-input
                  :sl-input-date   sl-input}}
  [{:name       ::color
    :type       :sl-color-picker
    :validation [{:spec      #(not (str/blank? %))
                  :error-msg [:div.error "Color must be picked"]}]}
   {:name       ::email
    :type       :sl-input-email
    :validation [{:spec      #(and
                               (string? %)
                               (str/includes? % "@"))
                  :error-msg [:div.error "Email must have an @ character"]}]}
   {:name ::number
    :type :sl-input-number}
   {:name ::date
    :type :sl-input-date}])
