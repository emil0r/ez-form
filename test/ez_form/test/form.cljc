(ns ez-form.test.form
  (:require #?@(:clj  [[clojure.test :refer [deftest is testing]]
                       [ez-form.core :as ez-form :refer [defform]]])
            #?@(:cljs [[cljs.test :refer-macros [deftest is testing]]
                       [ez-form.core :as ez-form :refer-macros [defform]]
                       [reagent.core :as r]])
            [clojure.zip :as zip]
            [ez-form.common :refer [get-field]]
            [ez-form.decorate :as decorate]
            [ez-form.flow :as flow]
            [ez-form.zipper :refer [zipper]]
            [vlad.core :as vlad]))


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
    :error-messages {:vlad.core/present (fn [_ _] "foobar")}}
   {:type :password
    :label (t :form.field/password)
    :name :password
    :validation (vlad/join
                 (vlad/attr [:password]
                            (vlad/present))
                 (vlad/attr [:password]
                            (vlad/length-in 6 100)))}
   {:type :multiselect
    :label "Multi select"
    :name :multi
    :help "Help text"
    :text "Text info"
    :options [[1 "One"]
              [2 "Two"]
              [3 "Three"]
              [4 "Four"]
              [5 "Five"]
              [6 "Six"]
              [7 "Seven"]
              [8 "Eight"]
              [9 "Nine"]
              [10 "Ten"]]
    :sort-by first}
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

(deftest validity
  (testing "Validity"
   (is (true? (ez-form/valid? (testform {} {:__ez-form.form-name "testform"
                                            :email "test@example.com"
                                            :password "my password"})))
       "Is valid")

   (is (false? (ez-form/valid? (testform {} {:email "test@example.com"
                                             :password "my password"})))
       "Is not valid because it's missing the form name")

   (is (false? (ez-form/valid? (testform {} {:__ez-form.form-name "testform"
                                             :email "test@example.com"
                                             :password ""})))
       "Is not valid because password is empty")))


(deftest errormessages
  (testing "Error messages"
    (is (=
         (list
          [:input {:type "hidden", :name "__ez-form.form-name", :value "testform"}]
          (list [:div.error "foobar"]))

         (ez-form/as-flow [:$email.errors] (testform nil))))))




(comment
  (ez-form/as-table (testform {} {:email "test@example.com"
                                  :password "my password"
                                  :sex "m"}))

  (ez-form/as-paragraph (testform {} {:email "test@example.com"
                                      :password "my password"
                                      :sex "m"}))


  (ez-form/as-list (testform {} {:email "test@example.com"
                                 :password "my password"
                                 :sex "m"}))

  (:options (testform nil))


  (ez-form/as-flow
   [:table.table
    [:tr :?email.wrapper
     [:th :$email.label]
     [:td.testus :$email.field :$email.errors]]]
   (testform {:email "test@example.com"} {} {:decor {:?wrapper nil}}))

  (ez-form/as-flow
   [:table.table
    [:tr :?male.wrapper
     [:th :$male.label]
     [:td.testus :$male.field :$male.errors]]
    [:tr :?female.wrapper
     [:th :$female.label]
     [:td.testus :$female.field :$female.errors]]]
   (testform {:email "test@example.com"} {}))


  (ez-form/as-template [:div :?wrapper [:span :$label] [:div.input :$field] :$help]
                       (testform {:email "test@example.com"} {}))

  (ez-form/as-template
   [:div
    [:span.label :$label]
    :$errors
    [:div.input :$field]]
   (testform {}))


  (get-field (testform {:email "test@example.com"}) :email.field)

  (decorate/decorate (testform nil) [:?email.wrapper])

  )
