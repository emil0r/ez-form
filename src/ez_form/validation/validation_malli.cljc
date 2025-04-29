(ns ez-form.validation.validation-malli
  (:require [malli.core :as malli]))

(defn validate [field {:keys [field/value] :as ctx}]
  (assoc field :errors (concat
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
                             (remove nil?)))))
