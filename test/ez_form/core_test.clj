(ns ez-form.core-test
  (:require [clojure.string :as str]
            [expectations.clojure.test :refer :all]
            [ez-form.core :as sut]
            [ez-form.field :as field]
            [ez-form.validation]
            [ez-form.validation.validation-malli]
            [lookup.core :as lookup]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defexpect render-test
  (let [form {:meta   {:posted?   true
                       :fields    field/fields
                       :fns       {:fn/test (fn [_ _] "This is a meta function")}
                       :field-fns {:errors sut/render-field-errors
                                   :fn/t   (fn [_form _field [_ label]]
                                             (str/capitalize (name label)))}}
              :fields {::username {:type       :text
                                   :errors     ["Error 1"
                                                "Error 2"]
                                   :label      [:fn/t ::username]
                                   :help       [:div.help "My help text"]
                                   :attributes {:name        :username
                                                :value       "johndoe"
                                                :id          "testform-username"
                                                :placeholder :ui.username/placeholder}}}}]
    (expect
     [:div
      [:input {:type        :text
               :name        :username
               :id          "testform-username"
               :value       "johndoe"
               :placeholder :ui.username/placeholder}]]
     (sut/render form [:div
                       [::username]])
     "Field is rendered (field lookup)")
    (expect
     [:div
      [:input {:type        :text
               :name        :username
               :id          "testform-username"
               :value       "johndoe"
               :placeholder :ui.username/placeholder}]
      ;; this looks horrible, but it allows for arbitrary hiccup
      ;; inside errors
      '(([:div.error "Error 1"])
        ([:div.error "Error 2"]))]
     (sut/render form [:div
                       [::username]
                       [::username :errors
                        [:div.error :error]]])
     "Field is rendered with errors (:posted? is true)")
    (expect
     [:div
      [:input {:type        :text
               :name        :username
               :id          "testform-username"
               :value       "johndoe"
               :placeholder :ui.username/placeholder}]
      nil]
     (sut/render (assoc-in form [:meta :posted?] false)
                 [:div
                  [::username]
                  [::username :errors
                   [:div.error :error]]])
     "Field is rendered with no errors (:posted? is false)")
    (expect
     [:div
      [:input {:type        :text
               :name        :username
               :id          "testform-username"
               :value       "johndoe"
               :placeholder :ui.username/placeholder}]
      [:div.help "My help text"]]
     (sut/render form [:div
                       [::username]
                       [::username :help]])
     "field is rendered with :help (field :key lookup)")
    (expect
     [:div
      [:label {:for "testform-username"}
       "Username"]
      [:input {:type        :text
               :name        :username
               :id          "testform-username"
               :value       "johndoe"
               :placeholder :ui.username/placeholder}]
      [:div.help "My help text"]]
     (sut/render form [:div
                       [:label {:for [::username :attributes :id]}
                        [::username :label]]
                       [::username]
                       [::username :help]])
     "field is a :label that targets the field")
    (expect
     [:div
      [:input {:type        :text
               :name        :username
               :id          "testform-username"
               :value       "johndoe"
               :placeholder :ui.username/placeholder}]
      ;; :text does not exist in the field, so we want nil back
      nil]
     (sut/render form [:div
                       [::username]
                       [::username :text]])
     "field is rendered with :text (field :key lookup)")
    (expect
     [:div
      [:label
       [:input {:type        :text
                :name        :username
                :id          "testform-username"
                :value       "johndoe"
                :placeholder :ui.username/placeholder}]
       "Username"]]
     (sut/render form [:div
                       [:label
                        [::username]
                        [::username :label]]])
     "field is rendered with a field-fn [field :key lookup]")
    (expect
     [:div
      [:input {:type        :text
               :name        :username
               :id          "testform-username"
               :value       "johndoe"
               :placeholder :ui.username/placeholder}]
      "This is a meta function"]
     (sut/render form [:div
                       [::username]
                       [:fn/test]])
     "field is rendered with a meta function")))

(defexpect post-process-form-test
  (let [email          "john.doe@example.com"
        username       "john.doe"
        form           {:meta {:validation     :spec
                               :form-name      "test"
                               :validation-fns {:spec ez-form.validation/validate}
                               :field-data     {:username username}}
                        :fields
                        {::username {:type       :text
                                     :attributes {:placeholder :ui.username/placeholder}}
                         ::email    {:type       :email
                                     :attributes {:id          "email-id"
                                                  :placeholder :ui.email/placeholder}}}}
        processed-form (sut/post-process-form form {:email               email
                                                    :__ez-form_form-name "test"})]
    (expect
     {:name        :username
      :id          "test-username"
      :value       username
      :placeholder :ui.username/placeholder}
     (get-in processed-form [:fields ::username :attributes])
     "username has value given by [:meta :field-data :username]")
    (expect
     {:name        :email
      :id          "email-id"
      :value       email
      :placeholder :ui.email/placeholder}
     (get-in processed-form [:fields ::email :attributes])
     "email has all html attributes")))

(defexpect post-process-form-spec-test
  (let [user-error1    :error.username/must-exist
        email-error1   :error.email/must-exist
        email          "john.doe@example.com"
        form           {:meta {:validation     :spec
                               :validation-fns {:spec ez-form.validation/validate}
                               :form-name      "test"}
                        :fields
                        {::username {:type       :text
                                     :validation [{:spec      #(not (str/blank? %))
                                                   :error-msg user-error1}]
                                     :attributes {:name        :username
                                                  :placeholder :ui.username/placeholder}}
                         ::email    {:type       :email
                                     :validation [{:spec      string?
                                                   :error-msg email-error1}]
                                     :attributes {:name        :email
                                                  :placeholder :ui.email/placeholder}}}}
        processed-form (sut/post-process-form form {:username            ""
                                                    :email               email
                                                    :__ez-form_form-name "test"})]
    (expect
     [user-error1]
     (get-in processed-form [:fields ::username :errors])
     "username has one error")
    (expect
     []
     (get-in processed-form [:fields ::email :errors])
     "email has no errors")))

(defexpect post-process-form-malli-test
  (let [user-error1    :error.username/must-exist
        email-error1   :error.email/must-exist
        email          "john.doe@example.com"
        form           {:meta {:validation     :malli
                               :form-name      "test"
                               :validation-fns {:malli ez-form.validation.validation-malli/validate}}
                        :fields
                        {::username {:type       :text
                                     :validation [{:spec      [:fn #(not (str/blank? %))]
                                                   :error-msg user-error1}]
                                     :attributes {:name        :username
                                                  :placeholder :ui.username/placeholder}}
                         ::email    {:type       :email
                                     :validation [{:spec      :string
                                                   :error-msg email-error1}]
                                     :attributes {:name        :email
                                                  :placeholder :ui.email/placeholder}}}}
        processed-form (sut/post-process-form form {:username            ""
                                                    :email               email
                                                    :__ez-form_form-name "test"})]
    (expect
     [user-error1]
     (get-in processed-form [:fields ::username :errors]))
    (expect
     []
     (get-in processed-form [:fields ::email :errors]))))

(defexpect defform-test
  (sut/defform testform
    {}
    [{:name       ::username
      :help       [:i18n :ui.username/help]
      :validation [{:spec      #(not= % "foobar")
                    :error-msg [:div.error "Username cannot be foobar"]}]
      :type       :text}
     {:name  ::email
      :label [:i18n ::email-label]
      :type  :email}])
  (binding [*anti-forgery-token* "anti-forgery-token"]
    (let [form (testform {:username "foobar"}
                         {:__ez-form_form-name "testform"
                          :email               "john.doe@example.com"})]
      (expect
       "testform"
       (get-in form [:meta :form-name])
       "form-name is set based on the name of the form in defform")
      (expect
       "foobar"
       (get-in form [:fields ::username :value])
       "username's value is set on the data being sent in")
      (expect
       "john.doe@example.com"
       (get-in form [:fields ::email :value])
       "email's value is set on the params being sent in")
      (expect
       [::username ::email]
       (get-in form [:meta :field-order])
       "field order is set according to the order in which fields are sent in")
      (expect
       false
       (sut/valid? form)
       "Form is invalid - name is foobar which breaks the validation for ::username")
      (expect
       "Username"
       (->> (sut/as-table form)
            (lookup/select '[th])
            (first)
            (lookup/text))
       "th value for ::username is the capitalized name of ::username")
      (expect
       (str ::email-label)
       (->> (sut/as-table form)
            (lookup/select '[th])
            (second)
            (lookup/text))
       "th value for ::email is the keyword ::email-label (lookup picks it up with text)")
      (expect
       ["testform" "anti-forgery-token" "foobar" "john.doe@example.com"]
       (->> (sut/as-table form)
            (lookup/select '[input])
            (map (comp :value second)))
       "field values show up in as-table")
      (expect
       [:div {:class #{"error"}}
        "Username cannot be foobar"]
       (->> (sut/as-table form)
            (lookup/select ["div.error"])
            (first))
       "Error shows up in as-table")
      (expect
       {:table-by-correct-class?   true
        :table-by-incorrect-class? false}
       (let [hiccup (sut/as-table form {:attributes {:class ["table"]}})]
         {:table-by-correct-class?   (->> hiccup
                                          (lookup/select ["table[class=table]"])
                                          (seq)
                                          (some?))
          :table-by-incorrect-class? (->> hiccup
                                          (lookup/select ["table[class=faulty-css]"])
                                          (seq)
                                          (some?))})
       "as-table correctly injects table-opts")
      (expect
       [:i18n :ui.username/help]
       (let [hiccup (sut/as-table form {:row-layout (fn [field-k]
                                                      [:tr
                                                       [:th
                                                        [field-k :label]]
                                                       [:td
                                                        [field-k]
                                                        [field-k :help]
                                                        [field-k :errors :error]]])})]
         (->> hiccup
              (lookup/select '[i18n])
              (first)))
       "as-table rendered with an alternative row layout")
      (expect
       {:username "foobar"
        :email    "john.doe@example.com"}
       (sut/fields->map form)
       "fields->map on the form gives me a map of all values for the fields in the form")
      (expect
       ["testform" "anti-forgery-token" "foobar" "john.doe@example.com"]
       (->> (sut/as-template form [:div.layout
                                   [:field]
                                   [:field :errors :error]])
            (lookup/select '[input])
            (map (comp :value second)))
       "field values show up in as-template")
      (expect
       [:div {:class #{"error"}}
        "Username cannot be foobar"]
       (->> (sut/as-template form [:div.layout
                                   [:field]
                                   [:field :errors :error]])
            (lookup/select ["div.error"])
            (first))
       "Error shows up in as-template"))))

(defexpect defform-changed-defaults-test
  (sut/defform testform
    {:extra-fns {:fn/anti-forgery nil}}
    [{:name       ::username
      :validation [{:spec      #(not= % "foobar")
                    :error-msg [:div.error "Username cannot be foobar"]}]
      :type       :text}
     {:name  ::email
      :label [:i18n ::email-label]
      :type  :email}])
  (let [form (testform {:username "foobar"}
                       {:__ez-form_form-name "testform"
                        :email               "john.doe@example.com"})]
    (expect
     ["testform" "foobar" "john.doe@example.com"]
     (->> (sut/as-table form)
          (lookup/select '[input])
          (map (comp :value second)))
     "field values show up in as-table")
    (expect
     ["testform" "foobar" "john.doe@example.com"]
     (->> (sut/as-template form [:div.layout
                                 [:field]
                                 [:field :errors :error]])
          (lookup/select '[input])
          (map (comp :value second)))
     "field values show up in as-template")))
