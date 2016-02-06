(ns ez-form.core
  (:require [ez-form.error :refer [get-error-message]]
            [ez-form.table :as table]
            [vlad.core :as vlad]))


(defn validate
  "Take a form, loop through the parameters and add error messages as we go"
  [fields params]
  (reduce (fn [out field]
            (let [{:keys [validation error-messages]} field]
              (if (and params validation)
                (let [validated (vlad/validate validation params)
                      errors (map (partial get-error-message field) validated)]
                  (conj out (assoc field
                                   :errors (if (empty? errors) nil errors)
                                   :validated validated)))
                (conj out field))))
          [] fields))

(defn valid? [form params]
  (every? nil? (map :errors (validate (:fields form) params))))


(defn- add-value [data {:keys [name] :as field}]
  (assoc field :value (get data name)))

(defrecord Form [fields options])

(defn form [fields options data params]
  (let [fields (if-not (nil? params)
                 (map (partial add-value params) fields)
                 (map (partial add-value data) fields))]
    (map->Form {:fields fields
                :options options})))

(defn as-table
  ([form]
   (map #(table/row % (:options form)) (:fields form)))
  ([form params]
   (map #(table/row % (:options form)) (validate (:fields form) params))))


(defmacro defform [name options fields]
  `(defn ~name
     ([~'data]
      (~name ~'data nil))
     ([~'data ~'params]
      (form ~fields ~options ~'data ~'params))))
