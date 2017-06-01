(ns ez-form.field.multiselect
  (:require [ez-form.common :as ez.common]
            [ez-form.field :as ez.field]))


(defn- multiselect-option [f opt]
  (let [[value description] (if (sequential? opt) opt [opt opt])]
    [:div {:key value :on-click (f value)} description]))

(defmethod ez.field/field :multiselect [field form-options]
  (let [id (ez.common/get-first field :id :name)
        opts (ez.field/get-opts field [:class :name] form-options)
        options (if (fn? (:options field))
                  ((:options field) field form-options)
                  (:options field))
        sorter (or (:sort-by field) second)
        [add-button remove-button] (or (:buttons field) ["»" "«"])
        c (:cursor field)
        add-fn (fn [value] (fn [e]
                            (swap! c clojure.set/union (set [value]))))
        remove-fn (fn [value] (fn [e]
                               (swap! c clojure.set/difference (set [value]))))]
    (when-not (set? @c)
      (reset! c (try (set @c)
                     (catch js/Error e
                       #{}))))
    [:table.multiselect (merge {:id id} opts)
     [:tbody
      [:tr
       [:td
        [:div.left
         (map #(multiselect-option add-fn %) (->> options
                                                  (filter #(not (some @c [(first %)])))
                                                  (sort-by sorter)))]]
       [:td
        [:div.move-left {:on-click #(reset! c #{})}
         remove-button]]
       [:td
        [:div.move-right {:on-click #(reset! c (into #{} (map first options)))}
         add-button]]
       [:td
        [:div.right
         (map #(multiselect-option remove-fn %) (->> options
                                                     (filter #(some @c [(first %)]))
                                                     (sort-by sorter)))]]]]]))
