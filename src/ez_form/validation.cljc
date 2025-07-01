(ns ez-form.validation
  (:require [clojure.spec.alpha :as spec]))

(defn validate [field {:keys [field/value] :as ctx}]
  (concat
   (->> (:validation field)
        (filter :spec)
        (map (fn [{:keys [spec error-msg]}]
               (when-not (spec/valid? spec value)
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
