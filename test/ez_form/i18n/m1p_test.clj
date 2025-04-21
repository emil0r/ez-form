(ns ez-form.i18n.m1p-test
  (:require [expectations.clojure.test :refer :all]
            [ez-form.core :as sut]
            [lookup.core :as lookup]
            [m1p.core :as m1p]))


(def dictionaries
  {:en {:form/username "Username"
        :form/email    "Email"}
   :se {:form/username "Användarnamn"
        :form/email    "Emejl"}
   :no {:form/username "Brukernavn"
        :form/email    "Epost"}})

(defn get-dictionaries [dictionaries locale]
  {:dictionaries {:i18n (locale dictionaries)}})

(defexpect m1p-test
  (sut/defform testform
    {}
    [{:name       ::username
      :label      [:i18n :form/username]
      :validation [{:spec      #(not= % "foobar")
                    :error-msg [:div.error "Username cannot be foobar"]}]
      :type       :text}
     {:name  ::email
      :label [:i18n :form/email]
      :type  :email}])
  (let [hiccup (sut/as-table (testform {:username "foobar"}
                                       {:__ez-form.form-name "testform"
                                        :email               "john.doe@example.com"}))]
    (expect
     ["Username" "Email"]
     (->> (m1p/interpolate hiccup (get-dictionaries dictionaries :en))
          (lookup/select '[label])
          (map last))
     ":en locale")
    (expect
     ["Användarnamn" "Emejl"]
     (->> (m1p/interpolate hiccup (get-dictionaries dictionaries :se))
          (lookup/select '[label])
          (map last))
     ":se locale")
    (expect
     ["Brukernavn" "Epost"]
     (->> (m1p/interpolate hiccup (get-dictionaries dictionaries :no))
          (lookup/select '[label])
          (map last))
     ":no locale")))
