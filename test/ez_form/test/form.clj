(ns ez-form.test.form
  (:require [ez-form.core :as ez-form :refer [defform]]
            [vlad.core :as vlad]
            [midje.sweet :refer :all]))


(defn t [arg]
  arg)

(defform testform
  {:css {:input {:all :form-control
                 :email "form-control email"}
         :label {:radio "btn btn-info"
                 :radio-active "btn btn-info active"}}}


  [{:type :email
    :label (t :form.field/email)
    :name :email
    :placeholder (t :form.placeholder/email)
    :validation (vlad/present)
    :error-messages {:custom "foobar"}}
   {:type :password
    :label (t :form.field/password)
    :name :password
    :validation (vlad/join
                 (vlad/present)
                 (vlad/length-in 6 100))}
   {:type :password
    :label (t :form.field/repeat-password)
    :name :repeat-password
    :error-messages {:equals-field (t :form.error/fields-equal)}}
   {:type :text
    :name :text
    :initial "my initial text"
    }
   {:type :dropdown
    :label "my dropdown"
    :name :dropdown
    :options ["opt 1"
              "opt 2"
              "opt 3"]
    :initial "opt 2"}
   {:type :checkbox
    :name :checkbox
    :label "my checkbox"}
   {:type :radio
    :name :radio
    :options {"m" "Male"
              "f" "Female"}}


   ])

(first (ez-form/as-table
  (testform {} {:email "emil@emil0r.com"})
  {:email "emil@emil0r.com"}))

(ez-form/as-table
 (ez-form/form [{:type :email
                 :name :email
                 :label "Email"}] {} {:radio "m"} nil)
 {}
 )

(ez-form/as-table {:fields  [{:type :radio
                              :name :radio
                              :options {"m" "Male"
                                        "f" "Female"}}]
                   :options {}} )

;; (testform default-data)
;; (testform default-data params)
;; (testform default-data params options)

;; (ez-form/as-table (testform request default-data {:validate? false}))
;; (ez-form/as-list (testform request default-data {:validate? false}))
;; (ez-form/as-paragraphs (testform request default-data {:validate? false}))
