(ns ez-form.test.core
  (:require [ez-form.core :as form]
            [ez-form.test.form :refer [testform]]
            [reagent.core :as r]))


(enable-console-print!)


(defn render-form [data]
  [:table
   [:tbody (form/as-table (testform data))]])


(defn ^:export run []
  (println "ez-form.test.core is running")
  (let [data (r/atom nil)]
    (r/render-component [render-form data] (.getElementById js/document "form-container"))))
