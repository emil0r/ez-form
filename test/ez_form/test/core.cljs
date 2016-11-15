(ns ez-form.test.core
  (:require [ez-form.core :as form]
            [ez-form.test.form :refer [testform]]
            [reagent.core :as r]))


(enable-console-print!)


(defn render-form [data result-fn]
  [:table
   [:tbody (form/as-table (testform data result-fn))]])


(defn ^:export run []
  (println "ez-form.test.core is running")
  (let [data (r/atom nil)
        result-fn (fn [data] (println (:status data)))]
    (r/render-component [render-form data result-fn] (.getElementById js/document "form-container"))))
