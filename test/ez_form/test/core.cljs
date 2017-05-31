(ns ez-form.test.core
  (:require [ez-form.core :as form]
            [ez-form.test.form :refer [testform]]
            [reagent.core :as r]))


(enable-console-print!)


(defn render-form [data result-fn]
  [:div
   [:button {:on-click (fn [e] (println (:fields @data)))} "Print all fields"]
   [:table
    [:tbody (form/as-table (testform data result-fn))]]])

(defn ^:export run []
  (println "ez-form.test.core is running")
  (let [data (r/atom {:fields {:fileuploader ["img/150x150.png"
                                              "img/350x150.png"
                                              "img/400x500.png"
                                              "img/40x100.png"]}})
        result-fn (fn [data] (println (:status data)))]
    (r/render-component [render-form data result-fn] (.getElementById js/document "form-container"))))
