(ns ez-form.error
  (:require [ez-form.i18n :refer [*t* *locale*]]))


(defmulti get-error-message (fn [_ _ error] (:type error)))

(defmethod get-error-message :vlad.core/present [form field error]
  (let [error-message (-> field :error-messages :vlad.core/present)]
    (or (if (fn? error-message)
          (error-message form field ::present)
          error-message)
        (*t* *locale* ::present))))

(defmethod get-error-message :vlad.core/length-under [form field error]
  (let [error-message (-> field :error-messages :vlad.core/length-under)]
    (or (if (fn? error-message)
          (error-message form field ::length-under (:size error))
          error-message)
        (*t* *locale* ::length-under (:size error)))))

(defmethod get-error-message :vlad.core/length-over [form field error]
  (let [error-message (-> field :error-messages :vlad.core/length-over)]
    (or (if (fn? error-message)
          (error-message form field ::length-over (:size error))
          error-message)
        (*t* *locale* ::length-over (:size error)))))

(defmethod get-error-message :vlad.core/equals-field [form field error]
  (let [error-message (-> field :error-messages :vlad.core/equals-field)]
    (or (if (fn? error-message)
          (error-message form field ::equals-field (-> error :second-selector first name))
          error-message)
        (*t* *locale* ::equals-field (-> error :second-selector first name)))))


(defmethod get-error-message :vlad.core/matches [form field error]
  (let [error-message (-> field :error-messages :vlad.core/matches)]
    (or (if (fn? error-message)
          (error-message form field ::matches (-> error :pattern str))
          error-message)
        (*t* *locale* ::matches (-> error :pattern str)))))

(defmethod get-error-message :vlad.core/equals-value [form field error]
  (let [error-message (-> field :error-messages :vlad.core/equals-value)]
    (or (if (fn? error-message)
          (error-message form field ::equals-value (-> error :value))
          error-message)
        (*t* *locale* ::equals-value (-> error :value)))))

;; skip any error messages if the error is nil
(defmethod get-error-message nil [form field error]
  (if-not (nil? error)
    (*t* *locale* ::unknown-error)))

(defmethod get-error-message :default [form field error]
  [field error]
  (let [error-message (or (get-in field [:error-messages (:type error)])
                          (:message error))]
    (or (if (fn? error-message)
          (error-message form field ::unknown-error)
          error-message)
        (*t* *locale* ::unknown-error))))
