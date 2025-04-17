(ns ez-form.core
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [ez-form.field :as field]))

(defn valid?
  ([form]
   (every? empty? (map :errors (vals (:fields form))))))

(defn post-process-form [form params]
  (let [validate-fn (requiring-resolve
                     (get (get-in form [:meta :validation-fns])
                          (get-in form [:meta :validation] :spec)
                          'ez-form.validation/validate))
        posted?     (= (:__ez-form.form-name params)
                       (get-in form [:meta :form-name]))]
    (reduce (fn [form [field-k field]]
              (let [field-name (get-in field [:attributes :name]
                                       (keyword (name field-k)))
                    value      (if posted?
                                 (or (get params field-name)
                                     (get-in form [:meta :field-data field-name]))
                                 (get-in form [:meta :field-data field-name]))]
                (assoc-in form [:fields field-k]
                          (-> field
                              (assoc-in [:attributes :value] value)
                              (assoc-in [:attributes :name] field-name)
                              (assoc :value value)
                              (validate-fn value)))))
            form (:fields form))))

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
            (get-in form [:fields (first x)]))
       (field/render (get-in form [:fields (first x)]))

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
              (get-in form [:fields field-kw :errors])))

       :else
       x))
   layout))

(defn ->form
  "Create a form"
  [opts fields params]
  (post-process-form {:meta   opts
                      :fields fields}
                     params))

(defn- assert-fields [form-name fields]
  (let [faulty-fields (remove #(qualified-keyword? (:name %)) fields)]
    (when (seq faulty-fields)
      (throw (ex-info "Some fields for did not have qualified keywords as names"
                      {:form-name form-name
                       :fields    (map :name faulty-fields)})))))

(defn raw-data->field-data [data]
  (->> data
       (map (fn [[k v]]
              (if (qualified-keyword? k)
                [(keyword (name k)) v]
                [k v])))
       (into {})))

(defmacro defform
  "Define a form. The form becomes a function that you can call with
   everything setup using ->form"
  [form-name form-opts fields]
  (assert-fields form-name fields)
  (let [form-name*  (name form-name)
        fields*     (->> fields
                         (map (juxt :name #(dissoc % :name)))
                         (into (sorted-map)))
        field-order (mapv :name fields)]
    ;; TODO: Fix linting
    `(defn ~form-name

       ([~'data]
        (~form-name nil ~'data nil))
       ([~'data ~'params]
        (~form-name nil ~'data ~'params))
       ([~'opts ~'data ~'params]
        (->form (merge
                 {:form-name       ~form-name*
                  :field-data      (raw-data->field-data ~'data)
                  :field-order     ~field-order
                  :validation      :spec
                  :validations-fns {:spec  'ez-form.validation/validate
                                    :malli 'ez-form.validation.validation-malli/validate}}
                 ~form-opts
                 ~'opts)
                ~fields*
                ~'params)))))
