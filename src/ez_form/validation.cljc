(ns ez-form.validation
  (:require [clojure.spec.alpha :as spec]))

(defn validate [field value]
  (assoc field :errors (->> (:validation field)
                            (map (fn [{:keys [spec error-msg]}]
                                   (when-not (spec/valid? spec value)
                                     error-msg)))
                            (remove nil?))))
