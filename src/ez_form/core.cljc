(ns ez-form.core
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [ez-form.field :as field]))

(defn anti-forgery [_ _]
  (require 'ring.middleware.anti-forgery)
  [:input {:id    :__anti-forgery-token
           :name  :__anti-forgery-token
           :value (force ring.middleware.anti-forgery/*anti-forgery-token*)
           :type  :hidden}])

(defn- get-field-name [field-k field]
  (get-in field [:attributes :name]
          (keyword (name field-k))))

(defn fields->map
  "Return the fields of the form as a map"
  [form]
  (->> (:fields form)
       (map (fn [[field-k field]]
              [(get-field-name field-k field)
               (:value field)]))
       (into {})))

(defn valid?
  ([form]
   (every? empty? (map :errors (vals (:fields form))))))

(defn- is-posted? [form params]
  (= (:__ez-form_form-name params)
     (get-in form [:meta :form-name])))

(defn post-process-form [form params]
  (let [validate-fn (requiring-resolve
                     (get (get-in form [:meta :validation-fns])
                          (get-in form [:meta :validation] :spec)
                          'ez-form.validation/validate))
        posted?     (is-posted? form params)]
    (-> (reduce (fn [form [field-k field]]
                  (let [field-name (get-field-name field-k field)
                        label      (get-in field [:label]
                                           (str/capitalize (name field-name)))
                        value      (if posted?
                                     (or (get params field-name)
                                         (get-in form [:meta :field-data field-name]))
                                     (get-in form [:meta :field-data field-name]))]
                    (assoc-in form [:fields field-k]
                              (-> field
                                  (assoc-in [:attributes :value] value)
                                  (assoc-in [:attributes :name] field-name)
                                  (assoc :value value)
                                  (assoc :label label)
                                  (validate-fn value)))))
                form (:fields form))
        (assoc-in [:meta :posted?] posted?))))

(defn- walk-errors [layout error]
  (walk/postwalk (fn [x]
                   (if (= :error x)
                     error
                     x))
                 layout))

(defn render-field-errors [_form field layout]
  (map #(walk-errors (drop 2 layout) %)
       (:errors field)))

(defn render [form layout]
  (walk/postwalk
   (fn [x]
     (cond
       ;; meta functions
       (and (vector? x)
            (keyword? (first x))
            (get-in form [:meta :fns (first x)]))
       (let [f (get-in form [:meta :fns (first x)])]
         (f form x))

       ;; render field
       (and (vector? x)
            (qualified-keyword? (first x))
            (= 1 (count x))
            (get-in form [:fields (first x)]))
       (field/render (get-in form [:fields (first x)]))

       ;; render lookup
       (and (vector? x)
            (qualified-keyword? (first x))
            (= (count x) 2)
            (not= :errors (second x))
            (get-in form (into [:fields] x)))
       ;; TODO: Write test for this piece of logic
       (let [value (get-in form (into [:fields] x))]
         (if (and (vector? value)
                  (get-in form [:meta :field-fns (first value)]))
           (let [f     (get-in form [:meta :field-fns (first value)])
                 field (get-in form [:fields (first x)])]
             (f form field value))
           value))

       ;; render field functions
       (and (vector? x)
            (qualified-keyword? (first x))
            (>= (count x) 2)
            (get-in form (into [:fields] (take 2 x)))
            (get-in form [:meta :field-fns (second x)]))
       (cond (and (= :errors (second x))
                  (false? (get-in form [:meta :posted?])))
             nil

             :else
             (let [f     (get-in form [:meta :field-fns (second x)])
                   field (get-in form [:fields (first x)])]
               (f form field x)))

       :else
       x))
   layout))

(defn form-name-input [form]
  [:input {:type  :hidden
           :value (get-in form [:meta :form-name])
           :name  :__ez-form_form-name}])

(defn as-table
  "Render the form as a table"
  ([form]
   (as-table form nil nil))
  ([form table-opts]
   (as-table form table-opts nil))
  ([form table-opts meta-opts]
   (let [form        (update form :meta merge meta-opts)
         field-order (get-in form [:meta :field-order])]
     (render
      form
      (list
       (form-name-input form)
       [:fn/anti-forgery]
       [:table table-opts
        [:tbody
         (map (fn [field-k]
                [:tr
                 [:th
                  [field-k :label]]
                 [:td
                  [field-k]
                  [field-k :errors :error]]])
              field-order)]])))))

(defn as-template
  "Render the form according to the template layout"
  ([form template-layout]
   (as-template form template-layout nil))
  ([form template-layout meta-opts]
   (let [form        (update form :meta merge meta-opts)
         field-order (get-in form [:meta :field-order])]
     (->> field-order
          (map (fn [field-k]
                 (walk/postwalk (fn [x]
                                  (if (= x :field)
                                    field-k
                                    x))
                                template-layout)))
          (concat [(form-name-input form)
                   [:fn/anti-forgery]])
          (render form)))))

(defn ->form
  "Create a form"
  [opts fields params]
  (post-process-form {:meta   opts
                      :fields fields}
                     params))

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
  [form-name meta-opts fields]
  (let [form-name*           (name form-name)
        fields*              (->> fields
                                  (map (juxt :name #(dissoc % :name)))
                                  (into (sorted-map)))
        field-order          (mapv :name fields)
        meta-opts-from-macro meta-opts]
    ;; TODO: Fix linting
    `(defn ~form-name
       ([~'data]
        (~form-name nil ~'data nil))
       ([~'data ~'params]
        (~form-name nil ~'data ~'params))
       ([~'meta-opts ~'data ~'params]
        (->form (merge
                 {:form-name       ~form-name*
                  :field-data      (raw-data->field-data ~'data)
                  :field-order     ~field-order
                  :field-fns       {:errors render-field-errors}
                  :fns             {:fn/anti-forgery anti-forgery}
                  :validation      :spec
                  :validations-fns {:spec  'ez-form.validation/validate
                                    :malli 'ez-form.validation.validation-malli/validate}}
                 ~meta-opts-from-macro
                 ~'meta-opts)
                ~fields*
                ~'params)))))
