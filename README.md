# ez-form

Forms for the web.

## Usage

```clojure
(ns some-namespace
  (:require [ez-form.core :as form :refer [defform]]
            [vlad.core :as vlad]))
  
(defform myform
 {}
 [{:type :email
   :name :myemail
   :label "My email"
   :validation (vlad/attr [:email] (vlad/present))}])
   
(form/as-table (myform {}))
(form/as-paragraph (myform {}))
(form/as-list (myform {}))
```

## License

Copyright © 2015-2016 Emil Bengtsson

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
