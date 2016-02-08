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
    :validation (vlad/attr [:email] (vlad/present))
    :error-messages {:custom "foobar"}}
   {:type :password
    :label (t :form.field/password)
    :name :password
    :validation (vlad/join
                 (vlad/attr [:password]
                            (vlad/present))
                 (vlad/attr [:password]
                            (vlad/length-in 6 100)))}
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

(fact
 "valid?"
 (ez-form/valid? (testform {} {:email "emil@emil0r.com"
                               :password "my password"}))
 => true

 (ez-form/valid? (testform {} {:email "emil@emil0r.com"
                               :password ""}))
 => false)
