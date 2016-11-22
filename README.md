# ez-form

Forms for the web. For Clojure.

## Dependancy

```clojure
[ez-form "0.7.0"]

;; development
[ez-form "0.7.1-SNAPSHOT"]
```

## Usage

Clojure works as below. 

ClojureScript works roughly the same way, with the exception of how the form is initiated. When initating a ClojureScript form it assumes the following:

1. You are using reagent
2. You are sending in as first argument a ratom
3. You are sending in as a second argument a function that will be called when the form is valid. The function will only be called once per valid confirmation.

```clojure
(ns some-namespace
  (:require [ez-form.core :as form :refer [defform]]
            [vlad.core :as vlad]))
  
(defform myform
 {:css {:field {:all :form-control
                :email "form-control email"}
 [{:type :text
   :label "First name"
   :name :firstname
   :opts {:order 1} ;; additional parameters to add to the output HTML
   :validation (vlad/attr [:firstname] (vlad/present))
   :error-messages {:custom "foobar"
                    :vlad.core/present (fn [form field] "my custom error message")}}
  {:type :text
   :label "Last name"
   :name :lastname
   :validation (vlad/attr [:lastname] (vlad/present))}
  {:type :email
   :name :myemail
   :label "My email"
   :help "Help text about email"
   :validation (vlad/attr [:email] (vlad/present))}])

;; using defform
;; given above myform defined by defform
(myform [default-data] [default-data params] [default-data params options])

;; the form as a table   
(form/as-table (myform {}))

;; the form as paragraphs
(form/as-paragraph (myform {}))

;; the form as a list
(form/as-list (myform {}))

;; the form based on a template. this template is used for each field in the form
;; takes :$[label, errors, field, text, help]

(form/as-template
  [:div
    [:span.label :$label]
    :$errors
    [:div.input :$field]]
  (myform {}))

;; free flow. takes :$<field-name>.[label, errors, field, text, help]
;; notice that you can optionally skip the $ in front of the field name
;; it's there mostly to help spot the field-name
(form/as-flow
  [:div.columns
    [:div.left
      :$email.label]
    [:div.right
      :$email.errors
      :$email.field
      [:p.text :$email.text]
      [:div.help
        :$email.help]]
  (myform {:email "emil@emil0r.com"}))
  
```

## fields

Special types

- :checkbox
- :boolean (same as :checkbox)
- :radio
- :html (will take a :fn field which assumes a function that takes [field form-options] as paramters)
- :dropdown (same format as hiccup)

Everything else goes into the :default multimethod for ez-form.field/field where the :type is used as the type of the field.

## help, text, label and error-messages

Help, text, label and error-messages can take functions as values. During evaluation the function for help, text and label will be called with two arguments, form and the current field. Error-messages will be called with at least 3 arguments: form, field, error key and any number of args. Whatever is returned will be used.

```clojure
(def locale (atom :en))

(defn delayed-t [k]
  (fn [form field]
    (t @locale k)))
    
(defn alt-delayed-t [k]
  (fn [form field k & args]
    ;; get the locale from data sent in to the form as opposed to relying on a
    ;; global atom, with a default locale of :en
    (apply t (get-in form [:options :data :locale] :en) k args)))

(defform i18n-form
 {}
 [{:name :name
   :type :text
   :label (delayed-t :form.field/name)
   :validation (vlad/attr [:name] (vlad/present))
   :error-messages {:vlad.core/present (delayed-t :form.field/error)}}])
  
```

## helper functions

```clojure

;; will return a map of all the fields along with their associated values
(form/select-fields (myform nil {:firstname "Firstname", :lastname "Lastname", :myemail "firstname@lastname.com"}))

```

## decor

ez-form has a concept of decor for decorating the markup being returned with optional classes/markup and allowing for a post-process of the output.
See ez-form.decorate for implementation details. Decor must always be a keyword starting with a ? sign (ie, :?decor or :?my-decor, :?wrapper, etc)

Internal decors that are supported are :?wrapper, :?text, :?help, :?error and :?label

```clojure

;; flow

(form/as-flow
  [:div :?email.wrapper
    :$email.errors
    :$email.field
    :$email.help]
  (myform nil {}))

;; template

(form/as-template
  [:div :?wrapper
    :$errors
    :$field
    :$help]
  (myform nil {}))
```

What will happen here is that :?wrapper will be replaced with {:class "error"} **in the event** that errors do exists. If no errors exists it will be removed. 
Internally ez-form use decor for text, help and errors allowing for overriding them.

Decor also allows for overriding the content that is used for decorating. In the event of :?wrapper you can do the following:

```clojure

;; setting it in the form

(defform myform
  {:decor {:?wrapper {:class "my-class"}}}
  [ fields ... ])

;; new CSS class

(form/as-flow
  [:div :?email.wrapper
    :$email.errors
    :$email.field
    :$email.help]
  (myform nil {} {:decor {:?wrapper {:class "new-class"}}}))

;; removing the wrapper. doesn't make a lot of sense for as-template, but it's a viable option
;; for as-table, as-paragraph, as-list

(form/as-flow
  [:div :?email.wrapper
    :$email.errors
    :$email.field
    :$email.help]
  (myform nil {} {:decor {:?wrapper {:class nil}}})) 
```

## validation
Uses [vlad](https://github.com/logaan/vlad) for validation. See documentation there. Creating new validation fields is done by following vlad's documentation.

```clojure
(let [default-data {}
      params {:firstname "Emil"
              :lastname "Bengtsson"
              :email "emil@emil0r.com"}]
  ;; returns give true
  (form/valid? (myform default-data parmas)))
```

## i18n
Uses a very simple implementation meant to be switched for something else. [Tower](https://github.com/ptaoussanis/tower) is recommended.


*Alternative 1*

```clojure
;; import a tower t function that you've set up
'(require [namespace.i18n :refer [t]])

(defn my-t-func [locale path & args]
  (apply t locale path args))
  
(binding [ez-form.error/*t* my-t-func]
  ;; do your stuff with ez-form within here
  )
```

*Alternative 2*

```clojure
;; import a tower t function that you've set up
'(require [namespace.i18n :refer [t]])

(defn- ez-form-t [locale path & args]
  ;; in this scenario we handle locale differently
  ;; and so don't even both with what is sent in
  (apply t path args))

;; wrap ez-form in a ring middleware
(defn wrap-ez-form-i18n [handler]
  (fn [request]
    (binding [ez-form.error/*t* ez-form-t]
      (handler request))))
```

## License

Copyright © 2015-2016 Emil Bengtsson

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.


---

Coram Deo
