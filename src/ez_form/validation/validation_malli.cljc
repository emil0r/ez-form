(ns ez-form.validation.validation-malli
  (:require [malli.core :as malli]))

(defn validate [field {:keys [field/value] :as ctx}]
  (concat
   (->> (:validation field)
        (filter :spec)
        (map (fn [{:keys [spec error-msg]}]
               (when-not (malli/validate spec value)
                 error-msg)))
        (remove nil?))
   (->> (:validation field)
        (filter :external)
        (map (fn [{:keys [external error-msg]}]
               (when-not (external field ctx)
                 error-msg)))
        (remove nil?))
   (->> (:validation field)
        (filter :complex)
        (map (fn [{:keys [complex] :as validation-map}]
               {:validation-map validation-map
                :result         (complex field (merge ctx validation-map))}))
        (remove #(nil? (:result %))))))
