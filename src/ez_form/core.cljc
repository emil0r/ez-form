(ns ez-form.core
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [ez-form.field :as field]))

(defn post-process-form [form params]
  (let [validate-fn (requiring-resolve
                     (get (get-in form [:meta :validation-fns])
                          (get-in form [:meta :validation] :spec)
                          'ez-form.validation/validate))]
    (reduce (fn [form [field-k field]]
              (when-let [value (get params (get-in field [:attributes :name]))]
                (assoc-in form [:ez-form/fields field-k]
                          (-> field
                              (assoc-in [:attributes :value] value)
                              (assoc :value value)
                              (validate-fn value)))))
            form (:ez-form/fields form))))

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
