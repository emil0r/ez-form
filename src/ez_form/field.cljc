(ns ez-form.field)


(defn input-field [{:keys [type attributes]}]
  [:input (merge attributes
                 {:type (or type :text)})])

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
             :checkbox       input-field
             :color          input-field
             :date           input-field
             :datetime-local input-field
             :email          input-field
             :file           input-field
             :hidden         input-field
             :month          input-field
             :number         input-field
             :password       input-field
             :radio          input-field
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
  [field fields]
  (if-let [field-fn (fields (:type field))]
    (field-fn field)
    (str "I am missing the field " (pr-str (or (get-in field [:attributes :name])
                                               field)))))
