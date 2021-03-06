(ns ez-form.core
  (:require [ez-form.common :as common]
            [ez-form.decorate :refer [decorate]]
            [ez-form.error :refer [get-error-message]]
            #?@(:cljs [[ez-form.field.datepicker]
                       [ez-form.field.datetimepicker]
                       [ez-form.field.fileuploader]
                       [ez-form.field.multiselect]
                       [ez-form.field.timepicker]])
            [ez-form.flow :as flow]
            [ez-form.keywordize :refer [keyify]]
            [ez-form.list :as list]
            [ez-form.paragraph :as paragraph]
            [ez-form.table :as table]
            [ez-form.zipper :refer [zipper]]
            #?(:cljs [reagent.core :as r])
            [vlad.core :as vlad]))

#?(:clj
   (defn post?
     "Is the request a POST?"
     [request]
     (= (:request-method request) :post)))

#?(:clj
   (defn get?
     "Is the request a GET?"
     [request]
     (= (:request-method request) :get)))

(defn validate
  "Take a form and validate it. Error messages are added to each field found to have errors"
  ([form]
   (if-let [params (:params form)]
     (validate form params)
     form))
  ([form params]
   (let [params (keyify params)]
     #?(:clj (assoc form
                    :params params
                    :fields
                    (reduce (fn [out field]
                              (let [{:keys [validation error-messages]} field]
                                (if (and params validation)
                                  (let [validated (vlad/validate validation params)
                                        errors (map #(get-error-message form field %) validated)]
                                    (conj out (assoc field
                                                     :errors (if (empty? errors) nil errors)
                                                     :validated validated)))
                                  (conj out field))))
                            [] (:fields form))))
     #?(:cljs (let [errors (reduce (fn [out field]
                                     (let [{:keys [name validation error-messages]} field]
                                       (if (and params validation)
                                         (let [validated (vlad/validate validation params)
                                               errors (map #(get-error-message form field %) validated)]
                                           (assoc out name errors))
                                         out)))
                                   {} (:fields form))]
                (reset! (:errors form) errors)
                form)))))

(defn valid?
  "Is the form valid? Runs a validate and checks for errors"
  ([form]
   #?(:clj (and
            (= (get-in form [:params :__ez-form.form-name])  (get-in form [:options :name]))
            (every? nil? (map :errors (:fields (validate form))))))
   #?(:cljs (every? empty? (vals @(:errors form)))))
  ([form params]
   #?(:clj (and
            (= (get-in params [:__ez-form.form-name])  (get-in form [:options :name]))
            (every? nil? (map :errors (:fields (validate form params))))))
   #?(:cljs (every? empty? (vals @(:errors form))))))


#?(:clj
   (defn- add-value [data {:keys [name] :as field}]
     (assoc field :value-added (get data name))))

#?(:cljs
   (defn- add-cursor [data {:keys [name] :as field}]
     (assoc field :cursor (r/cursor data [:fields name]))))

#?(:cljs
   (defn- add-errors [data {:keys [name] :as field}]
     (assoc field :errors (r/cursor data [name]))))

#?(:cljs
   (defn- track-focus [form]
     (let [form-fn (:fn form)]
       (fn [k _ old-state new-state]
         (when (fn? form-fn)
           (let [old-errors @(:errors form)]
             (validate form (:fields new-state))
             ;; emit state?
             (if (valid? form)
               (form-fn
                {:status :valid
                 :form form})
               (form-fn
                {:status :invalid
                 :form form}))))))))

(defrecord Form [fields options data params])

(defn form [fields form-options data params-or-fn options]
  #?(:clj
     (let [params (keyify params-or-fn)
           fields (if-not (nil? params-or-fn)
                    (map #(add-value params %) fields)
                    (map #(add-value data %) fields))
           form (map->Form {:fields fields
                            :options (assoc form-options :data options)
                            :data data
                            :params params})]
       (cond
         (false? (:validation? options)) form
         params (validate form)
         :else form)))
  #?(:cljs
     (let [errors (r/atom nil)
           fields (->> fields
                       (map #(add-cursor data %))
                       (map #(add-errors errors %)))
           params (r/cursor data [:fields])
           form (map->Form {:fields fields
                            :options (assoc form-options :data options)
                            :data data
                            :params @params
                            :errors errors
                            :fn params-or-fn})]

       (when-not (:initialized? @data)
         (swap! data assoc
                :initialized? true
                :fields (assoc (:fields @data) :__ez-form.form-name (:name form-options)))
         (add-watch data :track-focus (track-focus form)))

       (validate form))))

(defn get-tail [form args]
  (->> (into args (if (get-in form [:options :name])
                    [:?ez-form.form-name]))
       (remove nil?)))

(defn as-table
  "Output the form as a table (wrap in a table)"
  [form & args]
  (let [tail (get-tail form args)
        form (assoc-in form [:options ::as] :table)]
    (decorate form
              (into (map #(table/row % (:options form)) (:fields form))
                    tail))))

(defn as-paragraph
  "Output the form as a list of paragraphs"
  [form & args]
  (let [tail (get-tail form args)
        form (assoc-in form [:options ::as] :paragraph)]
    (decorate form
              (into (map #(paragraph/paragraph % (:options form)) (:fields form))
                    tail))))

(defn as-list
  "Out the form as a list (wrap in ul or ol list)"
  [form & args]
  (let [tail (get-tail form args)
        form (assoc-in form [:options ::as] :list)]
    (decorate form
              (into (map #(list/li % (:options form)) (:fields form))
                    tail))))

(def as-flow flow/flow)

(defn as-template
  "Apply the template to the fields of the form"
  [template form & args]
  (let [tail (get-tail form args)
        form (assoc-in form [:options ::as] :template)]
    (into
     (map (fn [field]
            (flow/flow (flow/correct-flowchart-for-template template field)
                       (assoc form :fields [field])))
          (:fields form))
     (decorate form tail))))

#?(:clj
(defn select-fields
  "Return the fields of the form as a map"
  [form]
  (reduce (fn [out field]
            (assoc out
                   (or (:id field) (:name field))
                   (or (:value-added field) (:value field))))
          {} (:fields form)))
)

#?(:cljs
(defn select-fields
  "Return the fields of the form as a map"
  [form]
  (let [ks (->> form
                :fields
                (map #(keyword (common/get-first % :name)))
                (into #{}))]
    (reduce (fn [out k]
              (assoc out k (get-in @(:data form) [:fields k])))
            {} ks)))
)

(defmacro defform [-name options fields]
  (let [form-name (name -name)]
    `(defn ~-name
       ([~'data]
        (~-name ~'data nil nil))
       ([~'data ~'params]
        (~-name ~'data ~'params nil))
       ([~'data ~'params ~'opts]
        (form ~fields (assoc ~options :name ~form-name) ~'data ~'params ~'opts)))))
