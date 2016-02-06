(ns ez-form.table
  (:require [clojure.string :as str]))

(defn- get-first [field & values]
  (loop [[value & values] values]
    (if-let [value (get field value)]
      (cond
       (keyword? value) (str/capitalize (name value))
       :else value)
      (if (and (nil? value) (nil? values))
        nil
        (recur values)))))

(defn- option [selected-value opt]
  (let [[value text] (if (sequential? opt)
                       [(first opt) (second opt)]
                       [opt opt])
        opts (if (= value selected-value)
               {:value value :selected true}
               {:value value})]
    [:option opts text]))

(defn- error-div [error]
  [:div.error error])

(defn- get-opts [field keys super-opts]
  (merge

   (:opts field)
   (into {} (map (fn [[k v]]
                   (if (nil? v)
                     [k v]
                     [k (if (keyword? v) (name v) v)])) (select-keys field keys)))))


(defmulti row (fn [field super-opts] (:type field)))

(defmethod row :checkbox [field super-opts]
  (let [label (get-first field :label :name)
        id (get-first field :id :name)
        value (:value field)
        text (:text field)
        checked? (:checked field)
        opts (get-opts field [:class :name :value :type] super-opts)
        errors (:errors field)]
    [:tr
     [:th [:label {:for id} label]]
     [:td
      (if errors
        (map error-div errors))
      [:input (merge opts {:id id} (if checked? {:checked true})) value]
      text]]))

(defmethod row :radio [field super-opts]
  (let [label (get-first field :label :name)
        id (get-first field :id :name)
        value (:value field)
        text (:text field)
        checked? (:checked field)
        opts (get-opts field [:class :name :value :type] super-opts)
        errors (:errors field)]
    [:tr
     [:th [:label {:for id} label]]
     [:td
      (if errors
        (map error-div errors))
      [:input (merge opts {:id id} (if checked? {:checked true})) value]
      text]]))

(defmethod row :textarea [field super-opts]
  (let [label (get-first field :label :name)
        id (get-first field :id :name)
        value (:value field)
        opts (get-opts field [:class :name] super-opts)
        errors (:errors field)]
    [:tr
     [:th [:label {:for id} label]]
     [:td
      (if errors
        (map error-div errors))
      [:textarea (merge opts {:id id}) value]]]))

(defmethod row :dropdown [field super-opts]
  (let [label (get-first field :label :name)
        id (get-first field :id :name)
        value (:value field)
        opts (get-opts field [:value :class :name] super-opts)
        options (:options field)
        errors (:errors field)]
    [:tr
     [:th [:label {:for id} label]]
     [:td
      (if errors
        (map error-div errors))
      [:select (merge opts {:type :select
                            :id id})
       (map (partial option value) options)]]]))

(defmethod row :default [field super-opts]
  "Any input that is like text will work with this one"
  (let [label (get-first field :label :name)
        id (get-first field :id :name)
        opts (get-opts field [:value :placeholder :class :name :type] super-opts)
        errors (:errors field)]
    [:tr
     [:th [:label {:for id} label]]
     [:td
      (if errors
        (map error-div errors))
      [:input (merge {:type :text
                      :id id} opts)]]]))
