(ns ez-form.test.form
  (:require [ez-form.common :refer [get-field]]
            [ez-form.core :as ez-form :refer [defform]]
            [ez-form.flow :as flow]
            [vlad.core :as vlad]
            [midje.sweet :refer :all]))


(defn t [arg]
  arg)

(defform testform
  {:css {:field {:all :form-control
                 :email "form-control email"
                 :dropdown nil}}}


  [{:type :email
    :label (t :form.field/email)
    :name :email
    :help "help text"
    :text "text info"
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
    :name :text}
   {:type :dropdown
    :label "my dropdown"
    :name :dropdown
    :options ["opt 1"
              "opt 2"
              "opt 3"]}
   {:type :checkbox
    :name :checkbox
    :label "my checkbox"}
   {:type :radio
    :name :sex
    :id :male
    :value "m"
    :label "Male"}
   {:type :radio
    :name :sex
    :id :female
    :value "f"
    :label "Female"}])

(ez-form/as-table (testform {} {:email "emil@emil0r.com"
                                :password "my password"
                                :radio "m"}))

(ez-form/as-paragraph (testform {} {:email "emil@emil0r.com"
                                    :password "my password"
                                    :radio "m"}))


(ez-form/as-list (testform {} {:email "emil@emil0r.com"
                               :password "my password"
                               :radio "m"}))


(ez-form/as-flow
 [:table.table
  [:tr :?email.wrapper
   [:th :$email.label]
   [:td.testus :$email.field :$email.errors]]]
 (testform {:email "emil@emil0r.com"} {} {:decor {:?wrapper nil}}))

(ez-form/as-flow
 [:table.table
  [:tr :?male.wrapper
   [:th :$male.label]
   [:td.testus :$male.field :$male.errors]]
  [:tr :?female.wrapper
   [:th :$female.label]
   [:td.testus :$female.field :$female.errors]]]
 (testform {:email "emil@emil0r.com"} {}))


(ez-form/as-template [:div :?wrapper [:span :$label] [:div.input :$field] :$help]
                     (testform {:email "emil@emil0r.com"} {}))

(ez-form/as-template
  [:div
    [:span.label :$label]
    :$errors
    [:div.input :$field]]
  (testform {}))


(get-field (testform {:email "emil@emil0r.com"}) :email.field)

(fact
 "valid?"
 (ez-form/valid? (testform {} {:__ez-form.form-name "testform"
                               :email "emil@emil0r.com"
                               :password "my password"}))
 => true

 (ez-form/valid? (testform {} {:email "emil@emil0r.com"
                               :password "my password"}))
 => false

 (ez-form/valid? (testform {} {:email "emil@emil0r.com"
                               :password ""}))
 => false)
