(ns ez-form.test.core
  (:require [ez-form.core :as form]
            [ez-form.test.form :refer [testform]]
            [reagent.core :as r]))


(enable-console-print!)


(defn render-form [f]
  (println "render-form")
  [:div
   [:div [:button
          {:on-click (fn [e] (println (form/select-fields f)))} "Print all fields"]]
   [:div [:button
          {:on-click (fn [e] (println @(:errors f)))} "Print all errors"]]
   [:table
    [:tbody (form/as-table f)]]])

(defn ^:export run []
  (println "ez-form.test.core is running")
  (let [data (r/atom {:fields {:should-not-show-up true
                               :fileuploader ["img/150x150.png"
                                              "img/350x150.png"
                                              "img/400x500.png"
                                              "img/40x100.png"]
                               ;;:date/picker #inst "2017-06-12"
                               :multi/select [2 4 6 8 10]}})
        result-fn (fn [data] (println (:status data)))
        f (testform data result-fn)]
    (println "run")
    (r/render-component [render-form f] (.getElementById js/document "form-container"))))
