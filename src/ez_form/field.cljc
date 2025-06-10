(ns ez-form.field)


(defn input-field [{:keys [type attributes]}]
  [:input (merge attributes
                 {:type type})])

(defn input-radio-field [{:keys [value options attributes]}]
  (map (fn [option]
         (let [[option-value option-label] (if (vector? option)
                                             option
                                             [option option])]
           [:label
            [:input (merge attributes
                           {:type    :radio
                            :value   option-value
                            :checked (= option-value value)})]
            option-label]))
       options))

(defn input-checkbox-field [{:keys [value options attributes]}]
  (let [values (set (if (sequential? value)
                      value
                      [value]))]
    (map (fn [option]
           (let [[option-value option-label] (if (vector? option)
                                               option
                                               [option option])]
             [:label
              [:input (merge attributes
                             {:type    :checkbox
                              :value   option-value
                              :checked (contains? values option-value)})]
              option-label]))
         options)))

(defn input-boolean-field [{:keys [value attributes]}]
  (let [value (case value
                "true" true
                true   true
                false)]
    [:label
     [:input (merge attributes
                    {:type    :checkbox
                     :value   (str (not value))
                     :checked (true? value)})]]))

(defn textarea-field [{:keys [attributes]}]
  [:textarea (dissoc attributes :value)
   (:value attributes)])


(defn select-field [{:keys [attributes options]}]
  [:select (dissoc attributes :value)
   (map (fn [[value text]]
          [:option {:value    value
                    :selected (= value (:value attributes))}
           text])
        options)])


(def fields {:button         input-field
             :boolean        input-boolean-field
             :checkbox       input-checkbox-field
             :color          input-field
             :date           input-field
             :datetime-local input-field
             :email          input-field
             :file           input-field
             :hidden         input-field
             :month          input-field
             :number         input-field
             :password       input-field
             :radio          input-radio-field
             :range          input-field
             :reset          input-field
             :search         input-field
             :submit         input-field
             :tel            input-field
             :text           input-field
             :time           input-field
             :url            input-field
             :week           input-field
             :select         select-field
             :textarea       textarea-field})

(defn render
  "Render a field according to type"
  [field fields ctx]
  (if-let [field-fn (fields (:type field))]
    (field-fn (assoc field :ctx ctx))
    (str "I am missing the field " (pr-str (or (get-in field [:attributes :name])
                                               field)))))
