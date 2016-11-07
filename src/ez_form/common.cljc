(ns ez-form.common)

(defn get-field
  "Get the field in the form based on the marker"
  ([form marker] (get-field form marker false))
  ([form marker wrapper?]
   (let [re (if wrapper?
              #"^:(\?)?(.*)\..+"
              #"^:(\$)?(.*)\..+")
         field (keyword (last (re-find re (str marker))))]
     (->> form
          :fields
          (filter (fn [{:keys [id name]}]
                    (or (= id field)
                        (= name field))))
          first))))
