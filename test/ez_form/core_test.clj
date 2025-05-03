(ns ez-form.core-test
  (:require [clojure.string :as str]
            [expectations.clojure.test :refer :all]
            [ez-form.core :as sut]
            [ez-form.field :as field]
            [ez-form.namespace-for-test :refer [meta-opts meta-opts-faulty]]
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

(defexpect process-form-test
  (let [email          "john.doe@example.com"
        username       "john.doe"
        form           {:meta {:validation     :spec
                               :form-name      "test"
                               :validation-fns {:spec ez-form.validation/validate}
                               :field-data     {::username username}}
                        :fields
                        {::username     {:type       :text
                                         :name       :_username
                                         :attributes {:placeholder :ui.username/placeholder}}
                         ::email        {:type       :email
                                         :name       :_email
                                         :attributes {:id          "email-id"
                                                      :placeholder :ui.email/placeholder}}
                         ::repeat-email {:type       :email
                                         :name       :_repeat-email
                                         :attributes {:id          "email-id2"
                                                      :placeholder :ui.repeat-email/placeholder}
                                         :validation [{:external  (fn [_ {:keys [field/value fields]}]
                                                                    (= value (get-in fields [::email :value])))
                                                       :error-msg [:span {:class ["error"]} "Not the same email"]}]}
                         ::number       {:type   :number
                                         :name   :_number
                                         :coerce (fn [_ {:keys [field/value]}]
                                                   (parse-long value))}}}
        processed-form (sut/process-form form {:_email              email
                                               :_number             "1"
                                               :__ez-form_form-name "test"})]
    (expect
     {:name        :_username
      :id          "test-_username"
      :value       username
      :placeholder :ui.username/placeholder}
     (get-in processed-form [:fields ::username :attributes])
     "username has value given by [:meta :field-data :username]")
    (expect
     {:name        :_email
      :id          "email-id"
      :value       email
      :placeholder :ui.email/placeholder}
     (get-in processed-form [:fields ::email :attributes])
     "email has all html attributes")
    (expect
     [[:span {:class ["error"]} "Not the same email"]]
     (get-in processed-form [:fields ::repeat-email :errors])
     "repeat email has to have the same value as email")
    (expect
     1
     (get-in processed-form [:fields ::number :value])
     "number has been coerced")))

(defexpect process-form-spec-test
  (let [user-error1    :error.username/must-exist
        email-error1   :error.email/must-exist
        email          "john.doe@example.com"
        form           {:meta {:validation     :spec
                               :validation-fns {:spec ez-form.validation/validate}
                               :form-name      "test"}
                        :fields
                        {::username {:type       :text
                                     :name       :_username
                                     :validation [{:spec      #(not (str/blank? %))
                                                   :error-msg user-error1}]
                                     :attributes {:name        :username
                                                  :placeholder :ui.username/placeholder}}
                         ::email    {:type       :email
                                     :name       :_email
                                     :validation [{:spec      string?
                                                   :error-msg email-error1}]
                                     :attributes {:name        :email
                                                  :placeholder :ui.email/placeholder}}}}
        processed-form (sut/process-form form {:_username           ""
                                               :_email              email
                                               :__ez-form_form-name "test"})]
    (expect
     [user-error1]
     (get-in processed-form [:fields ::username :errors])
     "username has one error")
    (expect
     []
     (get-in processed-form [:fields ::email :errors])
     "email has no errors")))

(defexpect process-form-malli-test
  (let [user-error1    :error.username/must-exist
        email-error1   :error.email/must-exist
        email          "john.doe@example.com"
        form           {:meta {:validation     :malli
                               :form-name      "test"
                               :validation-fns {:malli ez-form.validation.validation-malli/validate}}
                        :fields
                        {::username {:type       :text
                                     :name       :_username
                                     :validation [{:spec      [:fn #(not (str/blank? %))]
                                                   :error-msg user-error1}]
                                     :attributes {:name        :username
                                                  :placeholder :ui.username/placeholder}}
                         ::email    {:type       :email
                                     :name       :_email
                                     :validation [{:spec      :string
                                                   :error-msg email-error1}]
                                     :attributes {:name        :email
                                                  :placeholder :ui.email/placeholder}}}}
        processed-form (sut/process-form form {:_username           ""
                                               :_email              email
                                               :__ez-form_form-name "test"})]
    (expect
     [user-error1]
     (get-in processed-form [:fields ::username :errors]))
    (expect
     []
     (get-in processed-form [:fields ::email :errors]))))

(defn coerce-number [_ {:keys [field/value]}]
  (parse-long value))

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
      :type  :email}
     {:name   ::number
      :coerce coerce-number
      :type   :number}])
  (binding [*anti-forgery-token* "anti-forgery-token"]
    (let [form (testform {::username "foobar"}
                         {:__ez-form_form-name         "testform"
                          :ez-form__!core-test_!email  "john.doe@example.com"
                          :ez-form__!core-test_!number "1"})]
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
       [::username ::email ::number]
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
       "the value for ::username is the capitalized name of ::username")
      (expect
       (str ::email-label)
       (->> (sut/as-table form)
            (lookup/select '[th])
            (second)
            (lookup/text))
       "the value for ::email is the keyword ::email-label (lookup picks it up with text)")
      (expect
       ["testform" "anti-forgery-token" "foobar" "john.doe@example.com" "1"]
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
       {::username "foobar"
        ::number   1
        ::email    "john.doe@example.com"}
       (sut/fields->map form)
       "fields->map on the form gives me a map of all values for the fields in the form")
      (expect
       ["testform" "anti-forgery-token" "foobar" "john.doe@example.com" "1"]
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

(defexpect defform-external-validation-test
  (let [db (atom {:!value "foobar"})]
    (sut/defform testform
      {}
      [{:name       ::username
        :help       [:i18n :ui.username/help]
        :validation [{:external  (fn [_field {:keys [db field/value]}]
                                   (not= (:!value @db) value))
                      :error-msg [:div.error "Username cannot be foobar"]}]
        :type       :text}
       {:name  ::email
        :label [:i18n ::email-label]
        :type  :email}])
    (binding [*anti-forgery-token* "anti-forgery-token"]
      (let [form (testform {::username "foobar"}
                           {:__ez-form_form-name        "testform"
                            :ez-form__!core-test_!email "john.doe@example.com"}
                           {:db db})]
        (expect
         false
         (sut/valid? form)
         "Form is invalid - name is foobar which breaks the validation for ::username")))))

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
  (let [form (testform {::username "foobar"}
                       {:__ez-form_form-name        "testform"
                        :ez-form__!core-test_!email "john.doe@example.com"})]
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

(defexpect defform-field-name-test
  (sut/defform testform
    {:extra-fns {:fn/anti-forgery nil}}
    [{:name ::first_name
      :type :text}
     {:name ::last_name
      :type :text}
     {:name :occupation
      :type :text}
     {:name :home
      :type :text}])
  (let [form (testform {::first_name "First name"
                        :home        "Pluto"}
                       {:__ez-form_form-name            "testform"
                        :ez-form__!core-test_!last_name "Last name"
                        :occupation                     "Hazard"})]
    (expect
     {::first_name "First name"
      ::last_name  "Last name"
      :occupation  "Hazard"
      :home        "Pluto"}
     (sut/fields->map form)
     "Various naming of fields works")))

(defn sl-input [{:keys [type attributes]}]
  (let [type* (name type)]
    [:sl-input (merge attributes
                      {:type (subs type* 9 (count type*))})]))

(def meta-opts-local {:extra-fields {:sl-input-text  sl-input
                                     :sl-input-email sl-input}})


(defexpect defform-meta-opts-test
  (sut/defform testform
    {:extra-fns            {:fn/foo identity}
     :extra-validation-fns {:validation/foo identity}
     :extra-fields         {:field/foo identity}
     :extra-field-fns      {:field-fn/foo identity}
     :validation           :validation/foo}
    [])
  (let [form (testform {})]
    (expect
     fn?
     (get-in form [:meta :fns :fn/foo])
     "fn/foo shows up in [:meta :fns :fn/foo]")
    (expect
     fn?
     (get-in form [:meta :validation-fns :validation/foo])
     "fn/foo shows up in [:meta :validation-fns :validation/foo]")
    (expect
     :validation/foo
     (get-in form [:meta :validation])
     ":validation/foo shows up in [:meta :validation]")
    (expect
     fn?
     (get-in form [:meta :fields :field/foo])
     ":field/foo shows up in [:meta :fields :field/foo]")
    (expect
     fn?
     (get-in form [:meta :field-fns :field-fn/foo])
     ":field-fn/foo shows up in [:meta :field-fn :field-fn/foo]"))

  (sut/defform testform2
    meta-opts
    [{:name :foo :type :text}])
  (let [form (testform2 {})]
    (expect
     fn?
     (get-in form [:meta :fns :fn/foo])
     "fn/foo shows up in [:meta :fns :fn/foo]")
    (expect
     fn?
     (get-in form [:meta :validation-fns :validation/foo])
     "fn/foo shows up in [:meta :validation-fns :validation/foo]")
    (expect
     :validation/foo
     (get-in form [:meta :validation])
     ":validation/foo shows up in [:meta :validation]")
    (expect
     fn?
     (get-in form [:meta :fields :field/foo])
     ":field/foo shows up in [:meta :fields :field/foo]")
    (expect
     fn?
     (get-in form [:meta :field-fns :field-fn/foo])
     ":field-fn/foo shows up in [:meta :field-fn :field-fn/foo]"))

  (sut/defform testform3
    ez-form.core-test/meta-opts-local
    [{:type :sl-input-text
      :name :name}
     {:type :sl-input-email
      :name :email}])

  (let [form (testform3 {})]
    (expect
     #{:sl-input-text
       :sl-input-email}
     (->> form :fields (vals) (map :type) set)))

  (sut/defform testform4
    meta-opts-local
    [{:type :sl-input-text
      :name :name}
     {:type :sl-input-email
      :name :email}])

  (let [form (testform4 {})]
    (expect
     #{:sl-input-text
       :sl-input-email}
     (->> form :fields (vals) (map :type) set))))


(defexpect process-field-test
  (expect
   [:foo {:name :foo}]
   (sut/process-field {:name :foo}))
  (expect
   [:foo.bar/baz {:name :foo__!bar_!baz}]
   (sut/process-field {:name :foo.bar/baz})))


(comment

  (sut/defform testform5
    meta-opts-faulty
    [{:type :sl-input-text
      :name ::name}
     {:type :sl-input-email
      :name ::email}])

  (sut/defform testform5
    meta-opts
    [{:type :text
      :name ::name}
     {:type :sl-input-email
      :name ::email}])
  )
