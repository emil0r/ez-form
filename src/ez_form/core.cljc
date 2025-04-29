(ns ez-form.core
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [ez-form.field :as field]
            [ez-form.validation])
  #?(:cljs (:require-macros [ez-form.core])))

(defn anti-forgery [#?(:clj _form :cljs form) _]
  #?(:clj
     (do (require 'ring.middleware.anti-forgery)
         [:input {:id    :__anti-forgery-token
                  :name  :__anti-forgery-token
                  :value (force @(resolve 'ring.middleware.anti-forgery/*anti-forgery-token*))
                  :type  :hidden}])
     :cljs
     [:input {:id    :__anti-forgery-token
              :name  :__anti-forgery-token
              :value (get-in form [:meta :anti-forgery-token])
              :type  :hidden}]))

(defn input-form-name [form _]
  [:input {:type  :hidden
           :value (get-in form [:meta :form-name])
           :name  :__ez-form_form-name}])


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
  (let [validate-fn (get (get-in form [:meta :validation-fns])
                         (get-in form [:meta :validation] :spec))
        posted?     (is-posted? form params)]
    (when (nil? validate-fn)
      (throw (ex-info "Missing validate-fn" {:validation     (get-in form [:meta :validation] :spec)
                                             :validation-fns (get-in form [:meta :validation-fns])})))
    (-> (reduce (fn [form [field-k field]]
                  (let [field-name (get-field-name field-k field)
                        field-id   (get-in field [:attributes :id]
                                           (str (get-in form [:meta :form-name])
                                                "-"
                                                (name field-name)))
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
                                  (assoc-in [:attributes :id] field-id)
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
            (keyword? (first x))
            (= 1 (count x))
            (get-in form [:fields (first x)]))
       (field/render (get-in form [:fields (first x)])
                     (get-in form [:meta :fields]))

       ;; render field functions
       (and (vector? x)
            (get-in form (into [:fields] (take 2 x)))
            (get-in form [:meta :field-fns (second x)]))
       (cond (and (= :errors (second x))
                  (not (get-in form [:meta :posted?])))
             nil

             :else
             (let [f     (get-in form [:meta :field-fns (second x)])
                   field (get-in form [:fields (first x)])]
               (f form field x)))

       ;; render lookup
       (and (vector? x)
            (not= :errors (second x))
            (get-in form [:fields (first x)]))
       (let [value (get-in form (into [:fields] x))]
         (if (and (vector? value)
                  (get-in form [:meta :field-fns (first value)]))
           (let [f     (get-in form [:meta :field-fns (first value)])
                 field (get-in form [:fields (first x)])]
             (f form field value))
           value))

       :else
       x))
   layout))

(defn as-table
  "Render the form as a table"
  ([form]
   (as-table form nil nil))
  ([form table-opts]
   (as-table form table-opts nil))
  ([form table-opts meta-opts]
   (let [form        (update form :meta merge meta-opts)
         field-order (get-in form [:meta :field-order])
         row-layout  (:row-layout table-opts
                                  (fn [field-k]
                                    [:tr
                                     [:th
                                      [:label {:for [field-k :attributes :id]}
                                       [field-k :label]]]
                                     [:td
                                      [field-k]
                                      [field-k :errors :error]]]))]
     (render
      form
      (list
       [:fn/input-form-name]
       [:fn/anti-forgery]
       [:table (:attributes table-opts)
        [:tbody
         (map row-layout field-order)]])))))

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
          (concat [[:fn/input-form-name]
                   [:fn/anti-forgery]])
          (render form)))))

(defn ->form
  "Create a form"
  [opts fields params]
  (post-process-form {:meta   (-> opts
                                  (update :validation-fns merge (:extra-validation-fns opts))
                                  (update :fns merge (:extra-fns opts))
                                  (update :field-fns merge (:extra-field-fns opts))
                                  (dissoc :extra-fields :extra-validation-fns :extra-fns :extra-field-fns))
                      :fields fields}
                     params))

(defn raw-data->field-data
  "Transform data from namespaced keywords to keywords.
  wrap-keyword-params is typically not used with keyword namespaces, and so
  this is a sensible default for now"
  [data]
  (->> data
       (map (fn [[k v]]
              (if (qualified-keyword? k)
                [(keyword (name k)) v]
                [k v])))
       (into {})))

(defmacro defform
  "Define a form. `meta-opts` are static in defform.`"
  [form-name meta-opts fields]
  (assert (symbol? form-name) "form-name must be a symbol")
  (when (and (not (map? meta-opts))
             (symbol? meta-opts)
             (nil? (namespace meta-opts)))
    (throw (ex-info "meta-opts must be a fully namespaced reference" {})))
  (when (and (not (map? meta-opts))
             (symbol? meta-opts)
             (namespace meta-opts))
    (require (symbol (namespace meta-opts)))
    (let [meta-opts @(resolve meta-opts)]
      (assert (map? meta-opts) "meta-opts must be a map")))
  (when-not (symbol? meta-opts)
    (assert (map? meta-opts) "meta-opts must be a map"))
  (assert (vector? fields) "fields must be a vector")
  (assert (every? map? fields) "fields must be a vector of maps")
  (assert (every? :name fields) "Each field in fields must have a :name")
  (let [form-name*           (name form-name)
        field-types          (->> (keys field/fields)
                                  (concat (keys (:extra-fields meta-opts)))
                                  (set))
        fields*              (->> fields
                                  (map (juxt :name identity))
                                  (into (sorted-map)))
        field-order          (mapv :name fields)
        meta-opts-from-macro meta-opts]
    (let [diff (set/difference (set (map :type (vals fields*))) field-types)]
      (when (seq diff)
        (throw (ex-info (str "Unsupported field type(s): " diff) {:types diff}))))
    `(defn ~form-name
       "
  - `data`` is the form data you wish to use initially (database, etc)
  - `params`` is the form data that has arrived from outside (POST request, AJAX call, etc)
  - `meta-opts` control the form. See documentation for more info"
       ([~'data]
        (~form-name ~'data nil nil))
       ([~'data ~'params]
        (~form-name ~'data ~'params nil))
       ([~'data ~'params ~'meta-opts]

        (->form (merge
                 {:form-name      ~form-name*
                  :field-data     (raw-data->field-data ~'data)
                  :field-order    ~field-order
                  :field-fns      {:errors render-field-errors}
                  :fields         field/fields
                  :fns            {:fn/anti-forgery    anti-forgery
                                   :fn/input-form-name input-form-name}
                  :validation     :spec
                  :validation-fns {:spec ez-form.validation/validate}}
                 ~meta-opts-from-macro
                 ~'meta-opts)
                ~fields*
                ~'params)))))
