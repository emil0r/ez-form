(ns ez-form.error
  #?(:cljs
     (:require
      [goog.string :as gstring]
      [goog.string.format])))

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
                            ::unknown-error "Okänt fel"}
                       :de {::equals-field, "Dieses Feld muss entsprechen %s"
                            ::equals-value, "Dieses Feld muss genau entsprechen %s"
                            ::length-over, "Dieses Feld muss über %d Zeichen betragen"
                            ::length-under, "Dieses Feld muss unter %d Zeichen betragen"
                            ::matches, "Dieses Feld muss entsprechen %s"
                            ::present, "Pflichtfeld"
                            ::unknown-error, "Unbekannter Fehler"}}))
(def ^:dynamic *locale* :en)
#?(:clj  (defn ^:dynamic *t* [locale path & args]
           (apply format (get-in @dictionary [locale path] "") args)))
#?(:cljs (defn ^:dynamic *t* [locale path & args]
           (apply gstring/format (get-in @dictionary [locale path] "") args)))

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
  (let [error-message (get-in field [:error-messages (:type error)])]
    (or (if (fn? error-message)
          (error-message form field ::unknown-error)
          error-message)
        (*t* *locale* ::unknown-error))))
