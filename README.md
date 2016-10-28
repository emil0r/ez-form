# ez-form

Forms for the web. Server side only (so far).

## Dependancy

```clojure
[ez-form "0.6.0-SNAPSHOT"]
```

## Usage

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
                    :vlad.core/present "my custom error message"}}
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

## decor

ez-form has a concept of decor for decorating the markup being returned with optional classes/markup and allowing for a post-process of the output.
See ez-form.decorate for implementation details. Decor must always be a keyword starting with a ? sign (ie, :?decor or :?my-decor, :?wrapper, etc)

Internal decors that are supported are :?wrapper, :?text, :?help, :?error

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

## License

Copyright © 2015-2016 Emil Bengtsson

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.


---

Coram Deo
