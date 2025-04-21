(ns ez-form.i18n.tempura-test
  (:require [expectations.clojure.test :refer :all]
            [ez-form.core :as sut]
            [lookup.core :as lookup]
            [taoensso.tempura :as tempura :refer [tr]]))

(def dictionaries
  {:en {:form/username "Username"
        :form/email    "Email"}
   :se {:form/username "Användarnamn"
        :form/email    "Emejl"}
   :no {:form/username "Brukernavn"
        :form/email    "Epost"}})

(defn translate [form _field [_ i18n-k]]
  (let [locale (get-in form [:meta :locale])]
    (tr {:dict dictionaries}
        [locale]
        [i18n-k])))

(defexpect tempura-test
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
  (let [hiccup-en (sut/as-table (testform {:locale :en}
                                          {:username "foobar"}
                                          {:__ez-form.form-name "testform"
                                           :email               "john.doe@example.com"}))
        hiccup-se (sut/as-table (testform {:locale :se}
                                          {:username "foobar"}
                                          {:__ez-form.form-name "testform"
                                           :email               "john.doe@example.com"}))
        hiccup-no (sut/as-table (testform {:locale :no}
                                          {:username "foobar"}
                                          {:__ez-form.form-name "testform"
                                           :email               "john.doe@example.com"}))]
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
