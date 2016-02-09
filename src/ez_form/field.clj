(ns ez-form.field
  (:require [clojure.string :as str]))

(defn error-div [error]
  [:div.error error])

(defn get-first [field & [capitalize? & values]]
  (let [values (if (true? capitalize?)
                 values
                 (into [capitalize?] values))
        capitalize? (if (true? capitalize?) true false)]
   (loop [[value & values] values]
     (if-let [value (get field value)]
       (cond
         (keyword? value) (if capitalize?
                            (str/capitalize (name value))
                            (name value))
         :else value)
       (if (and (nil? value) (nil? values))
         nil
         (recur values))))))

(defn errors
  "Send in a field from a map and get back a list of error messages"
  [{:keys [errors]}]
  (if errors
    (map error-div errors)))

(defn label [field]
  (let [label (get-first field true :label :name)
        id (get-first field :id :name)]
    [:label {:for id} label]))

(defn text [field]
  (:text field))

(defn option [selected-value opt]
  (let [[value text] (if (sequential? opt)
                       [(first opt) (second opt)]
                       [opt opt])
        opts (if (= value selected-value)
               {:value value :selected true}
               {:value value})]
    [:option opts text]))

(defn get-opts [field keys form-options]
  (merge
   (if (contains? (get-in form-options [:css :field]) :all)
     {:class (get-in form-options [:css :field :all])})
   (if (contains? (get-in form-options [:css :field]) (:type field))
     {:class (get-in form-options [:css :field (:type field)])})
   (:opts field)
   (into {} (map (fn [[k v]]
                   (if (nil? v)
                     [k v]
                     [k (if (keyword? v) (name v) v)])) (select-keys field keys)))))


(defmulti field (fn [field form-options] (:type field)))

(defmethod field :checkbox [field form-options]
  (let [id (get-first field :id :name)
        value (:value-added field)
        checked? (:checked field)
        opts (get-opts field [:class :name :value :type] form-options)]
    [:input (merge {:value value} opts {:id id} (if (or checked?
                                                        (= value (:value opts)))
                                                  {:checked true}))]))

(defmethod field :radio [field form-options]
  (let [id (get-first field :id :name)
        value (:value-added field)
        checked? (:checked field)
        opts (get-opts field [:class :name :value :type] form-options)]
    [:input (merge {:value value} opts {:id id} (if (or checked?
                                                        (= value (:value opts)))
                                                  {:checked true}))]))

(defmethod field :textarea [field form-options]
  (let [id (get-first field :id :name)
        value (or (:value field) (:value-added field))
        opts (get-opts field [:class :name] form-options)]
    [:textarea (merge opts {:id id}) value]))

(defmethod field :dropdown [field form-options]
  (let [id (get-first field :id :name)
        value (or (:value field) (:value-added field))
        opts (get-opts field [:class :name] form-options)
        options (:options field)]
    [:select (merge opts {:type :select
                          :id id})
       (map (partial option value) options)]))

(defmethod field :default [field form-options]
  (let [id (get-first field :id :name)
        value (or (:value field) (:value-added field))
        opts (get-opts field [:placeholder :class :name :type] form-options)]
    [:input (merge {:type :text
                    :value value
                    :id id} opts)]))
