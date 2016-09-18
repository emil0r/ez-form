(ns ez-form.error)

(def dictionary (atom {:en {::present "This field is mandatory"
                            ::length-under "This field must be under %d characters"
                            ::length-over "This field must be over %d characters"
                            ::equals-field "This field must be equal to %s"
                            ::matches "This field must match %s"
                            ::equals-value "This field must exactly match %s"
                            ::unknown-error "Unknown error"}
                       :no {::present "Feltet er obligatorisk"
                            ::length-under "Feltet må være under %d tegnet"
                            ::length-over "Feltet må være over %d tegnet"
                            ::equals-field "Feltet må være likt felt %s"
                            ::matches "Feltet må tilsvare %s"
                            ::equals-value "Dette felt må være likt %s"
                            ::unknown-error "Ukjent feil"}
                       :sv {::present "Detta fält är obligatoriskt"
                            ::length-under "Detta fält måste vara under %d tecken"
                            ::length-over "Detta fält måste vara över %d tecken"
                            ::equals-field "Detta fält måste vara det samma som fältet %s"
                            ::matches "Detta fält måste motsvara %s"
                            ::equals-value "Detta fält måste vara exakt lika %s"
                            ::unknown-error "Okänt fel"}}))
(def ^:dynamic *locale* :en)
(defn ^:dynamic *t* [locale path & args]
  (apply format (get-in @dictionary [locale path] "") args))

(defmulti get-error-message (fn [_ error] (:type error)))

(defmethod get-error-message :vlad.core/present [field error]
  (let [error-message (-> field :error-messages :vlad.core/present)]
    (or error-message
        (*t* *locale* ::present))))

(defmethod get-error-message :vlad.core/length-under [field error]
  (let [error-message (-> field :error-messages :vlad.core/length-under)]
    (or error-message
        (*t* *locale* ::length-under (:size error)))))

(defmethod get-error-message :vlad.core/length-over [field error]
  (let [error-message (-> field :error-messages :vlad.core/length-over)]
    (or error-message
        (*t* *locale* ::length-over (:size error)))))

(defmethod get-error-message :vlad.core/equals-field [field error]
  (let [error-message (-> field :error-messages :vlad.core/equals-field)]
    (or error-message
        (*t* *locale* ::equals-field (-> error :second-selector first name)))))


(defmethod get-error-message :vlad.core/matches [field error]
  (let [error-message (-> field :error-messages :vlad.core/matches)]
    (or error-message
        (*t* *locale* ::matches (-> error :pattern str)))))

(defmethod get-error-message :vlad.core/equals-value [field error]
  (let [error-message (-> field :error-messages :vlad.core/equals-value)]
    (or error-message
        (*t* *locale* ::equals-value (-> error :value)))))

;; skip any error messages if the error is nil
(defmethod get-error-message nil [field error]
  (if-not (nil? error)
    (*t* *locale* ::unknown-error)))

(defmethod get-error-message :default [field error]
  [field error]
  (or (get-in field [:error-messages (:type error)])
      (*t* *locale* ::unknown-error)))
