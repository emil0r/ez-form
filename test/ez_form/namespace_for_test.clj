(ns ez-form.namespace-for-test)

(defn meta-opt-catchall-fn [& args])
(def meta-opts {:extra-fns            {:fn/foo meta-opt-catchall-fn}
                :extra-validation-fns {:validation/foo meta-opt-catchall-fn}
                :extra-fields         {:field/foo meta-opt-catchall-fn}
                :extra-field-fns      {:field-fn/foo meta-opt-catchall-fn}
                :validation           :validation/foo})

(def meta-opts-faulty [])
