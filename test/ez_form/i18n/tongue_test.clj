(ns ez-form.i18n.tongue-test
  (:require [expectations.clojure.test :refer :all]
            [ez-form.core :as sut]
            [lookup.core :as lookup]
            [tongue.core :as tongue]))

(def dictionaries
  {:en              {:form/username "Username"
                     :form/email    "Email"}
   :se              {:form/username "Användarnamn"
                     :form/email    "Emejl"}
   :no              {:form/username "Brukernavn"
                     :form/email    "Epost"}
   :tongue/fallback :en})

(def t
  (tongue/build-translate dictionaries))

(defn translate [form _field [_ i18n-k]]
  (let [locale (get-in form [:meta :locale])]
    (t locale i18n-k)))

(defexpect tongue-test
  (sut/defform testform
    {:field-fns {:fn/t translate}}
    [{:name       ::username
      :label      [:fn/t :form/username]
      :validation [{:spec      #(not= % "foobar")
                    :error-msg [:div.error "Username cannot be foobar"]}]
      :type       :text}
     {:name  ::email
      :label [:fn/t :form/email]
      :type  :email}])
  (let [hiccup-en (sut/as-table (testform {:username "foobar"}
                                          {:__ez-form.form-name "testform"
                                           :email               "john.doe@example.com"}
                                          {:locale :en}))
        hiccup-se (sut/as-table (testform {:username "foobar"}
                                          {:__ez-form.form-name "testform"
                                           :email               "john.doe@example.com"}
                                          {:locale :se}))
        hiccup-no (sut/as-table (testform {:username "foobar"}
                                          {:__ez-form.form-name "testform"
                                           :email               "john.doe@example.com"}
                                          {:locale :no}))]
    (expect
     ["Username" "Email"]
     (->> hiccup-en
          (lookup/select '[label])
          (map last))
     ":en locale")
    (expect
     ["Användarnamn" "Emejl"]
     (->> hiccup-se
          (lookup/select '[label])
          (map last))
     ":se locale")
    (expect
     ["Brukernavn" "Epost"]
     (->> hiccup-no
          (lookup/select '[label])
          (map last))
     ":no locale")))
