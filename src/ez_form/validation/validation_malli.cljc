(ns ez-form.validation.validation-malli
  (:require [malli.core :as malli]))

(defn validate [field value]
  (assoc field :errors (->> (:validation field)
                            (map (fn [{:keys [spec error-msg]}]
                                   (when-not (malli/validate spec value)
                                     error-msg)))
                            (remove nil?))))
