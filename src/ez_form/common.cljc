(ns ez-form.common
  (:require [clojure.string :as str]))

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

(defn get-first
  "Get the first value out of the keys sent in"
  [field & [capitalize? & ks]]
  (let [ks (if (true? capitalize?)
             ks
             (into [capitalize?] ks))
        capitalize? (if (true? capitalize?) true false)]
    (loop [[value & ks] ks]
      (if-let [value (get field value)]
        (cond
          (keyword? value) (if capitalize?
                             (str/capitalize (name value))
                             (name value))
          :else value)
        (if (and (nil? value) (nil? ks))
          nil
          (recur ks))))))
