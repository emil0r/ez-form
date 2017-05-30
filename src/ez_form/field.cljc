(ns ez-form.field
  (:require #?(:clj [clojure.edn :refer [read-string]])
            [clojure.string :as str]
            [ez-form.common :refer [get-first]]
            [ez-form.decorate :refer [add-decor
                                      add-error-decor
                                      add-help-decor
                                      add-label-decor
                                      add-text-decor]]
            #?@(:cljs [[cljs.reader :refer [read-string]]
                       [reagent.core :as r]])))

(defmulti ->transform (fn [field data] (:transform field)))
(defmethod ->transform :edn [_ data]
  (read-string data))
(defmethod ->transform :default [_ data]
  data)

(defn value-of [field element]
  (->transform field (-> element .-target .-value)))

(defn errors
  "Send in a field from a map and get back a list of error messages"
  [{:keys [errors] :as field}]
  ;; for clojure errors are always run per display
  #?(:clj  (if errors
             (map #(add-error-decor field %) errors)))
  ;; for clojurescript always decorate errors, we just
  ;; do not show them when they're not present
  #?(:cljs (add-decor :error field)))

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
        ;; have selected-value as a set
        ;; so that we can have multiple options
        ;; set as true
        selected-value (if (sequential? selected-value)
                         (set selected-value)
                         (set [selected-value]))
        opts
        #?(:clj  (if (some selected-value [value])
                   {:value value :selected true}
                   {:value value}))
        #?(:cljs (if (some selected-value [value])
                   {:value value :key value :selected true}
                   {:value value :key value}))]
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
                     [k (cond
                          (keyword? v) (name v)
                          (fn? v) (v field)
                          :else v)])) (select-keys field keys)))))


(defmulti field (fn [field form-options] (:type field)))

(defmethod field :checkbox [field form-options]
  #?(:clj
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
                                  (not (false? value-added))
                                  (= (:value field) value-added)))
                          {:checked true}))])))
  ;; leave multiple checkboxes for now
  #?(:cljs
     (let [id (get-first field :id :name)
           c (:cursor field)
           opts (get-opts field [:class :name :type] form-options)]
       [:input (merge {:on-change #(reset! c (not (true? @c)))
                       :checked (true? @c)}
                      opts
                      {:id id})])))

(defmethod field :boolean [f form-options]
  (field (assoc f :type :checkbox) form-options))

(defmethod field :radio [field form-options]
  (let [id (get-first field :id :name)
        checked? (:checked field)
        opts (get-opts field [:class :name :value :type] form-options)
        value (or (:value-added field) (:value field))
        #?@(:cljs [c (:cursor field)])]
    #?(:clj  [:input (merge {:value value} opts {:id id} (if (or checked?
                                                                 (= value (:value opts)))
                                                           {:checked true}))])
    #?(:cljs [:input (merge {:value @c
                             :checked (= @c (:value opts))
                             :on-change #(reset! c (value-of field %))} opts {:id id})])))

(defmethod field :html [field form-options]
  (if-let [f (:fn field)]
    (f field form-options)))

(defmethod field :textarea [field form-options]
  (let [id (get-first field :id :name)
        opts (get-opts field [:class :name] form-options)
        #?@(:clj  [value (or (:value field) (:value-added field))])
        #?@(:cljs [c (:cursor field)])]
    #?(:clj
       [:textarea (merge opts {:id id}) (or value "")])
    #?(:cljs
       [:textarea (merge opts {:id id :on-change #(reset! c (value-of field %)) :value (or @c "")})])))

(defmethod field :dropdown [field form-options]
  (let [id (get-first field :id :name)
        opts (get-opts field [:class :name] form-options)
        options (:options field)
        #?@(:clj  [value (or (:value field) (:value-added field) "")])
        #?@(:cljs [c (:cursor field)
                   cljs-opts (cond
                               (:multiple opts) {:on-change #(reset! c (into (or @c []) [(value-of field %)]))}
                               :else {:on-change #(reset! c (value-of field %))})])]
    [:select (merge opts
                    {:type :select
                     :id id}
                    #?(:cljs cljs-opts))
     #?(:cljs
        (if (fn? options)
          (map #(option @c %) (options (:data form-options)))
          (map #(option @c %) options)))
     #?(:clj
        (if (fn? options)
          (map #(option value %) (options (:data form-options)))
          (map #(option value %) options)))]))

(defmethod field :default [field form-options]
  (let [id (get-first field :id :name)
        opts (get-opts field [:placeholder :class :name :type] form-options)
        #?@(:clj  [value (or (:value field) (:value-added field))])
        #?@(:cljs [c (:cursor field)])]
    [:input (merge {:type :text
                    #?@(:clj [:value (or value "")])
                    #?@(:cljs [:value (or @c "")
                               :on-change #(reset! c (value-of field %))])
                    :id id} opts)]))
