(ns ez-form.error)

(defmulti get-error-message (fn [_ error] (:type error)))

(defmethod get-error-message :vlad.validations/present [field error]
  (let [error-message (-> field :error-messages :vlad.validations/present)]
    (or error-message
        ;;(t :form.error/present)
        :form.error/present
        )))

(defmethod get-error-message :vlad.validations/length-under [field error]
  (let [error-message (-> field :error-messages :vlad.validations/length-under)]
    (or error-message
        ;;(t :form.error/length-under (:size error))
        :form.error/length-under
        )))

(defmethod get-error-message :vlad.validations/length-over [field error]
  (let [error-message (-> field :error-messages :vlad.validations/length-over)]
    (or error-message
        ;;(t :form.error/length-over (:size error))
        :form.error/length-over
        )))

(defmethod get-error-message :vlad.validations/equals-field [field error]
  (let [error-message (-> field :error-messages :vlad.validations/equals-field)]
    (or error-message
        ;;(t :form.error/equals-field (-> error :second-selector first name))
        :form.error/equals-field
        )))


(defmethod get-error-message :vlad.validations/matches [field error]
  (let [error-message (-> field :error-messages :vlad.validations/matches)]
    (or error-message
        ;;(t :form.error/matches (-> error :pattern str))
        :form.error/matches
        )))


(defmethod get-error-message :vlad.validations/equals-value [field error]
  (let [error-message (-> field :error-messages :vlad.validations/equals-value)]
    (or error-message
        ;;(t :form.error/equals-value (-> error :value))
        :form.error/equals-value
        )))

(defmethod get-error-message :custom [field error]
  (let [error-message (-> field :error-messages :custom)]
    (or error-message
        ;;(t :form.error/unknown-error)
        :form.error/unknown-error
        )))

;; skip any error messages if the error is nil
(defmethod get-error-message nil [field error]
  (if-not (nil? error)
    nil
    ;;(t :form.error/unknown-error)
    )
  :form.error/unknown-error)

(defmethod get-error-message :default [field error]
  ;;(t :form.error/unknown-error)
  :form.error/unknown-error
  )
