(ns ez-form.core
  (:require [ez-form.error :refer [get-error-message]]
            [ez-form.flow :as flow]
            [ez-form.list :as list]
            [ez-form.paragraph :as paragraph]
            [ez-form.table :as table]
            [vlad.core :as vlad]))

(defn post?
  "Is the request a POST?"
  [request]
  (= (:request-method request) :post))

(defn get?
  "Is the request a GET?"
  [request]
  (= (:request-method request) :get))

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
                              errors (map (partial get-error-message field) validated)]
                          (conj out (assoc field
                                           :errors (if (empty? errors) nil errors)
                                           :validated validated)))
                        (conj out field))))
                  [] (:fields form)))))

(defn valid?
  "Is the form valid? Runs a validate and checks for errors"
  ([form]
   (every? nil? (map :errors (:fields (validate form)))))
  ([form params]
   (every? nil? (map :errors (:fields (validate form params))))))


(defn- add-value [data {:keys [name] :as field}]
  (assoc field :value-added (get data name)))

(defrecord Form [fields options])

(defn form [fields form-options data params options]
  (let [fields (if-not (nil? params)
                 (map #(add-value params %) fields)
                 (map #(add-value data %) fields))
        form (map->Form {:fields fields
                         :options form-options
                         :data data
                         :params params})]
    (cond
      (false? (:validation? options)) form
      params (validate form)
      :else form)))

(defn as-table [form]
  (map #(table/row % (:options form)) (:fields form)))

(defn as-paragraph [form]
  (map #(paragraph/paragraph % (:options form)) (:fields form)))

(defn as-list [form]
  (map #(list/li % (:options form)) (:fields form)))

(def as-flow flow/flow)

(defmacro defform [name options fields]
  `(defn ~name
     ([~'data]
      (~name ~'data nil nil))
     ([~'data ~'params]
      (~name ~'data ~'params nil))
     ([~'data ~'params ~'opts]
      (form ~fields ~options ~'data ~'params ~'opts))))
