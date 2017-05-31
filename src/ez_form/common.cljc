(ns ez-form.common
  (:require [clojure.string :as str]
            [ez-form.keywordize :refer [kw->string]]))

(defn get-keyword [marker wrapper?]
  (let [re (if wrapper?
              #"^:(\?)?(.*)\..+"
              #"^:(\$)?(.*)\..+")]
    (keyword (last (re-find re (str marker))))))

(defn get-field
  "Get the field in the form based on the marker"
  ([form marker] (get-field form marker false))
  ([form marker wrapper?]
   (let [kw (get-keyword marker wrapper?)]
     (->> form
          :fields
          (filter (fn [{:keys [id name]}]
                    (or (= id kw)
                        (= name kw))))
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
                             (kw->string value))

          (fn? value) (value field)

          :else value)
        (if (and (nil? value) (nil? ks))
          nil
          (recur ks))))))
