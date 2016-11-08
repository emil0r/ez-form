(ns ez-form.field
  (:require [clojure.string :as str]
            [ez-form.common :refer [get-first]]
            [ez-form.decorate :refer [add-error-decor
                                      add-help-decor
                                      add-label-decor
                                      add-text-decor]]))

(defn errors
  "Send in a field from a map and get back a list of error messages"
  [{:keys [errors] :as field}]
  (if errors
    (map #(add-error-decor field %) errors)))

(defn label [field]
  (add-label-decor field))

(defn text [field]
  (add-text-decor field))

(defn help [field]
  (add-help-decor field))

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
        value-added (:value-added field)
        checked? (:checked? field)
        options (:options field)
        opts (get-opts field [:class :name :value :type] form-options)]
    (if options
      (let [value-added (cond (string? value-added) (str/split value-added #",")
                              :else value-added)]
        (map (fn [option]
               (let [[value label] (if (sequential? option)
                                     option
                                     [option option])
                     id (str (name id) "-" value)]
                 [:div
                  [:input (merge {:value value}
                                 opts
                                 {:id id}
                                 (if (or
                                      ;; default checked, but only if
                                      ;; value-added is empty
                                      (and checked?
                                           (nil? value-added))
                                      ;; check if value equals value-added
                                      (some #(= value %) value-added))
                                   {:checked true}))]
                  [:label {:for id} label]]))
             options))
      [:input (merge {:value value-added}
                     opts
                     {:id id}
                     (if (or
                          ;; default checked, but only if value-added is empty
                          (and checked?
                               (nil? value-added))
                          ;; checked if value-added is non-nil and non-false
                          (and (not (nil? value-added))
                               (not (false? value-added))))
                       {:checked true}))])))

(defmethod field :boolean [f form-options]
  (field (assoc f :type :checkbox) form-options))

(defmethod field :radio [field form-options]
  (let [id (get-first field :id :name)
        value (:value-added field)
        checked? (:checked field)
        opts (get-opts field [:class :name :value :type] form-options)]
    [:input (merge {:value value} opts {:id id} (if (or checked?
                                                        (= value (:value opts)))
                                                  {:checked true}))]))

(defmethod field :html [field form-options]
  (if-let [f (:fn field)]
    (f field form-options)))

(defmethod field :textarea [field form-options]
  (let [id (get-first field :id :name)
        value (or (:value field) (:value-added field))
        opts (get-opts field [:class :name] form-options)]
    [:textarea (merge opts {:id id}) (or value "")]))

(defmethod field :dropdown [field form-options]
  (let [id (get-first field :id :name)
        value (or (:value field) (:value-added field))
        opts (get-opts field [:class :name] form-options)
        options (:options field)]
    [:select (merge opts {:type :select
                          :id id})
     (if (fn? options)
       (map (partial option value) (options (:data form-options)))
       (map (partial option value) options))]))

(defmethod field :default [field form-options]
  (let [id (get-first field :id :name)
        value (or (:value field) (:value-added field))
        opts (get-opts field [:placeholder :class :name :type] form-options)]
    [:input (merge {:type :text
                    :value (or value "")
                    :id id} opts)]))
