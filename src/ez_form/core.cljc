(ns ez-form.core
  (:require [ez-form.decorate :refer [decorate]]
            [ez-form.error :refer [get-error-message]]
            #?@(:cljs [[ez-form.field.fileuploader]
                       [ez-form.field.multiselect]])
            [ez-form.flow :as flow]
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
   (assoc form
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
                  [] (:fields form)))))

(defn valid?
  "Is the form valid? Runs a validate and checks for errors"
  ([form]
   (and
    ;; check for both keyword and string because ring's middleware for transforming
    ;; the params map to keyword/string pairs doesn't seem to work
    ;; with "_ez-form.form-name"
    #?(:clj (or (= (get-in form [:params "__ez-form.form-name"]) (get-in form [:options :name]))
                (= (get-in form [:params :__ez-form.form-name])  (get-in form [:options :name]))))
    (every? nil? (map :errors (:fields (validate form))))))
  ([form params]
   (and
    #?(:clj (or (= (get-in params ["__ez-form.form-name"]) (get-in form [:options :name]))
                (= (get-in params [:__ez-form.form-name])  (get-in form [:options :name]))))
    (every? nil? (map :errors (:fields (validate form params)))))))


#?(:clj
   (defn- add-value [data {:keys [name] :as field}]
     (assoc field :value-added (get data name))))

#?(:cljs
   (defn- add-cursor [data {:keys [id name] :as field}]
     (assoc field :cursor (r/cursor data [:fields name]))))

#?(:cljs
   (defn- track-focus [form]
     (fn [k _ old-state new-state]
       ;; emit state?
       (when (and (fn? (:form-fn new-state))
                  (valid? form (:fields new-state))
                  (not (valid? form (:fields old-state)))
                  ;;(true? (::form.valid? new-state))
                  ;;(not (::form.valid? old-state))
                  )
         ((:form-fn new-state)
          {:status :valid
           :data (:fields new-state)})))))

(defrecord Form [fields options data params])

(defn form [fields form-options data params-or-fn options]
  #?(:clj
     (let [fields (if-not (nil? params-or-fn)
                    (map #(add-value params-or-fn %) fields)
                    (map #(add-value data %) fields))
            form (map->Form {:fields fields
                             :options (assoc form-options :data options)
                             :data data
                             :params params-or-fn})]
        (cond
          (false? (:validation? options)) form
          params-or-fn (validate form)
          :else form)))
  #?(:cljs
     (let [fields (map #(add-cursor data %) fields)
           params (r/cursor data [:fields])
           form (map->Form {:fields fields
                            :options (assoc form-options :data options)
                            :data data
                            :params @params
                            :fn params-or-fn})]

       (when-not (:initialized? @data)
         (swap! data assoc
                :initialized? true
                :form-fn params-or-fn
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

(defn select-fields
  "Return the fields of the form as a map"
  [form]
  (reduce (fn [out field]
            (assoc out
                   (or (:id field) (:name field))
                   (or (:value-added field) (:value field))))
          {} (:fields form)))

(defmacro defform [-name options fields]
  (let [form-name (name -name)]
    `(defn ~-name
       ([~'data]
        (~-name ~'data nil nil))
       ([~'data ~'params]
        (~-name ~'data ~'params nil))
       ([~'data ~'params ~'opts]
        (form ~fields (assoc ~options :name ~form-name) ~'data ~'params ~'opts)))))
