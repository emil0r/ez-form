# ez-form

Forms for the web. Server side only (so far).

## Dependancy

```clojure
[ez-form "0.5.0"]
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
