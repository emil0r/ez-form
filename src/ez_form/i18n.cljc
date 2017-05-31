(ns ez-form.i18n
  #?(:cljs
     (:require
      [goog.string :as gstring]
      [goog.string.format])))


(def dictionary (atom {:en {:ez-form.error/present "This field is mandatory"
                            :ez-form.error/length-under "This field must be under %d characters"
                            :ez-form.error/length-over "This field must be over %d characters"
                            :ez-form.error/equals-field "This field must be equal to %s"
                            :ez-form.error/matches "This field must match %s"
                            :ez-form.error/equals-value "This field must exactly match %s"
                            :ez-form.error/unknown-error "Unknown error"

                            :ez-form.field.fileuploader/drop-file-here "Drop file here"}
                       :no {:ez-form.error/present "Feltet er obligatorisk"
                            :ez-form.error/length-under "Feltet må være under %d tegnet"
                            :ez-form.error/length-over "Feltet må være over %d tegnet"
                            :ez-form.error/equals-field "Feltet må være likt felt %s"
                            :ez-form.error/matches "Feltet må tilsvare %s"
                            :ez-form.error/equals-value "Dette felt må være likt %s"
                            :ez-form.error/unknown-error "Ukjent feil"

                            :ez-form.field.fileuploader/drop-file-here "Slipp filen her"}
                       :sv {:ez-form.error/present "Detta fält är obligatoriskt"
                            :ez-form.error/length-under "Detta fält måste vara under %d tecken"
                            :ez-form.error/length-over "Detta fält måste vara över %d tecken"
                            :ez-form.error/equals-field "Detta fält måste vara det samma som fältet %s"
                            :ez-form.error/matches "Detta fält måste motsvara %s"
                            :ez-form.error/equals-value "Detta fält måste vara exakt lika %s"
                            :ez-form.error/unknown-error "Okänt fel"

                            :ez-form.field.fileuploader/drop-file-here "Släpp filen här"}
                       :de {:ez-form.error/equals-field, "Dieses Feld muss entsprechen %s"
                            :ez-form.error/equals-value, "Dieses Feld muss genau entsprechen %s"
                            :ez-form.error/length-over, "Dieses Feld muss über %d Zeichen betragen"
                            :ez-form.error/length-under, "Dieses Feld muss unter %d Zeichen betragen"
                            :ez-form.error/matches, "Dieses Feld muss entsprechen %s"
                            :ez-form.error/present, "Pflichtfeld"
                            :ez-form.error/unknown-error, "Unbekannter Fehler"

                            :ez-form.field.fileuploader/drop-file-here "Dateien hier ablegen"}}))
(def ^:dynamic *locale* :en)
#?(:clj  (defn ^:dynamic *t* [locale path & args]
           (apply format (get-in @dictionary [locale path] "") args)))
#?(:cljs (defn ^:dynamic *t* [locale path & args]
           (apply gstring/format (get-in @dictionary [locale path] "") args)))
