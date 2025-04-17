(ns ez-form.core
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [ez-form.field :as field]))

(defn- walk-errors [layout error-kw error]
  (walk/postwalk (fn [x]
                   (if (= error-kw x)
                     error
                     x))
                 layout))

(defn render [form layout]
  (walk/postwalk
   (fn [x]
     (cond
       ;; render field
       (and (vector? x)
            (qualified-keyword? (first x))
            (get-in form [:ez-form/fields (first x)]))
       (field/render (get-in form [:ez-form/fields (first x)]))

       ;; render errors associated with field
       (and (vector? x)
            (qualified-keyword? (first x))
            (str/ends-with? (name (first x)) ".errors"))
       (let [name-str (subs (name (first x))
                            0
                            (str/index-of (name (first x)) ".errors"))
             field-kw (keyword (namespace (first x))
                               name-str)
             error-kw (keyword (namespace (first x))
                               (str name-str ".error"))]
         (map #(walk-errors (drop 1 x) error-kw %)
              (get-in form [:ez-form/fields field-kw :errors])))

       :else
       x))
   layout))
