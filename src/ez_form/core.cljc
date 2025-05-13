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


(defn fields->map
  "Return the fields of the form as a map"
  [form]
  (->> (:fields form)
       (map (fn [[field-k field]]
              [field-k
               (:value field)]))
       (into {})))

(defn valid?
  ([form]
   (every? empty? (vals (get-in form [:meta :errors])))))

(defn- is-posted? [form params]
  (or (true? (get-in form [:meta :process?]))
      (= (:__ez-form_form-name params)
         (get-in form [:meta :form-name]))))

(defn process-form
  "
  Process form. Sets it up to be rendered, validates, coerces, etc
  "
  [form params]
  (let [validate-fn (get (get-in form [:meta :validation-fns])
                         (get-in form [:meta :validation] :spec))
        posted?     (is-posted? form params)]
    (when (nil? validate-fn)
      (throw (ex-info "Missing validate-fn" {:validation     (get-in form [:meta :validation] :spec)
                                             :validation-fns (get-in form [:meta :validation-fns])})))
    (let [fields (->> (:fields form)
                      (map (fn [[field-k field]]
                             (let [field-name (:name field)
                                   field-id   (get-in field [:attributes :id]
                                                      (str (get-in form [:meta :form-name])
                                                           "-"
                                                           (name field-name)))
                                   label      (get-in field [:label]
                                                      (str/capitalize (name field-k)))
                                   value*     (if posted?
                                                (or (get params field-name)
                                                    (get-in form [:meta :field-data field-k]))
                                                (get-in form [:meta :field-data field-k]))
                                   value      (if-let [coerce-fn (:coerce field)]
                                                (coerce-fn field {:field/value value*})
                                                value*)]
                               [field-k (-> field
                                            (assoc-in [:attributes :value] value*)
                                            (assoc-in [:attributes :name] field-name)
                                            (assoc-in [:attributes :id] field-id)
                                            (assoc :value value
                                                   :label label
                                                   :field-k field-k))])))
                      (into {}))
          ;; do two passes on fields. one for updates, one for validations where
          ;; fields might depend on other fields
          errors (->> fields
                      (map (fn [[field-k field]]
                             [field-k (validate-fn field (merge {:field/value (:value field)
                                                                 :fields      fields}
                                                                (:meta form)))]))
                      (into {}))]
      (-> form
          (assoc-in [:meta :posted?] posted?)
          (assoc-in [:meta :errors] errors)
          (assoc :fields fields)))))

(defn- walk-errors [layout error]
  (walk/postwalk (fn [x]
                   (if (= :error x)
                     error
                     x))
                 layout))

(defn render-field-errors [form _field layout]
  (when (get-in form [:meta :posted?])
    (map #(walk-errors (drop 2 layout) %)
         (get-in form [:meta :errors (first layout)]))))

(defn render [form layout]
  ;; NOTE: :fields is a sorted-map, which require all keys
  ;; to be of the same sort
  ;; render field functions and render lookup need to
  ;; look for this
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
            (get-in form [:meta :field-fns (second x)]))
       (let [f     (get-in form [:meta :field-fns (second x)])
             field (get-in form [:fields (first x)])]
         (f form field x))

       ;; render lookup
       (and (vector? x)
            (not= :errors (second x))
            (keyword? (first x))
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
  ;; the meta update has to be in here, as we want to be able to override in runtime
  (process-form {:meta   (-> opts
                             (update :validation-fns merge (:extra-validation-fns opts))
                             (update :fns merge (:extra-fns opts))
                             (update :fields merge (:extra-fields opts))
                             (update :field-fns merge (:extra-field-fns opts))
                             (dissoc :extra-fields :extra-validation-fns :extra-fns :extra-field-fns))
                 :fields fields}
                params))

(defn process-field [{field-name :name :as field}]
  (if (qualified-keyword? field-name)
    ;; NOTE: Using str/replace caused an output in JS that halted
    ;; the execution of the rest of the script
    (let [new-name (-> (.. (subs (str field-name) 1 (count (str field-name)))
                           (replace "." "__!")
                           (replace "/" "_!"))
                       (keyword))]
      [field-name (assoc field :name new-name)])
    [field-name field]))

(defn reg-form [form-name meta-opts fields]
  (assert (map? meta-opts) (str "meta-opts must be a map. Is currently: " (type meta-opts)))
  (assert (vector? fields) "fields must be a vector")
  (assert (every? map? fields) "fields must be a vector of maps")
  (assert (every? :name fields) "Each field in fields must have a :name")
  (let [field-types  (->> (keys field/fields)
                          (concat (keys (:extra-fields meta-opts)))
                          (set))
        fields*      (->> fields
                          (map process-field)
                          (into (sorted-map)))
        field-lookup (->> fields*
                          (map (fn [[field-k {:keys [name]}]]
                                 [name field-k]))
                          (into {}))
        field-order  (mapv :name fields)]
    (let [diff (set/difference (set (map :type (vals fields*))) field-types)]
      (when (seq diff)
        (throw (ex-info (str "Unsupported field type(s): " diff) {:types diff}))))
    (fn form-fn
      ([data]
       (form-fn data nil nil))
      ([data params]
       (form-fn data params nil))
      ([data params meta-opts-runtime]
       (->form (merge
                {:form-name      form-name
                 :field-data     data
                 :field-order    field-order
                 :field-lookup   field-lookup
                 :field-fns      {:errors render-field-errors}
                 :fields         field/fields
                 :fns            {:fn/anti-forgery    anti-forgery
                                  :fn/input-form-name input-form-name}
                 :validation     :spec
                 :validation-fns {:spec ez-form.validation/validate}}
                meta-opts
                meta-opts-runtime)
               fields*
               params)))))

(defmacro defform
  "Define a form. `meta-opts` are static in defform.`"
  [form-name meta-opts fields]
  (assert (symbol? form-name) "form-name must be a symbol")
  (let [form-name* (name form-name)]
    `(def ~form-name
       "
  - `data`` is the form data you wish to use initially (comes from database, etc)
  - `params`` is the form data that has arrived from outside (POST request, AJAX call, etc)
  - `meta-opts` control the form. See documentation for more info"
       (reg-form ~form-name* ~meta-opts ~fields))))
