(ns ez-form.field.datepicker
  (:require [ez-form.common :as ez.common]
            [ez-form.field :as ez.field]
            [ez-form.i18n :refer [*t* *locale*]]
            [goog.events]
            [goog.dom]
            [goog.i18n.DateTimeFormat]
            [goog.i18n.DateTimeParse]
            [goog.ui.DatePicker]
            [goog.ui.DatePicker.Events]
            [goog.ui.InputDatePicker]
            [reagent.core :as r]))


(defn- handler [c]
  (fn [e]
    (reset! c (-> e .-target .-value))))

(defn anchor [id]
  [:div {:id (str "anchor-" id)}])

(def props (r/atom {}))

(defmulti <-goog-date type)
(defmethod <-goog-date :default [date]
  (if (some? date)
    ;; javascript... why oh why?!
    (goog.date.Date. (.getFullYear date) (.getMonth date) (.getDate date))
    nil))

(defn- handle-dp-event [dp c]
  (goog.events/listen
   dp
   goog.ui.DatePicker.Events/CHANGE
   (fn [e]
     (when (.-date e)
       (reset! c (goog-date-> (.-date e)))))))

(defn- get-props [field form-options]
  (let [id (ez.common/get-first field :id :name)]
    (if (some? (get-in @props [id]))
      (get @props id)
      (let [opts (ez.field/get-opts field [:class :name] form-options)
            c (:cursor field)
            goog-date-> (or (:goog-date-> field)
                            (fn [date]
                              (if (some? date)

                                (let [d (.-date date) ;; get the javascript date
                                      offset (-> date .-date .getTimezoneOffset) ;; get the offset
                                      ]
                                  ;; add the offset after its been reversed
                                  (.setMinutes d (+ (.getMinutes d) (* -1 offset)))
                                  d))))
            dp (cond (false? (:input? field))
                     (goog.ui.DatePicker.)

                     :else
                     (let [pattern (or (:pattern field)
                                       "yyyy'-'MM'-'dd")
                           formatter (goog.i18n.DateTimeFormat. pattern)
                           parser (goog.i18n.DateTimeParse. pattern)]
                       (goog.ui.InputDatePicker. formatter parser)))
            f (cond (false? (:input? field))
                    (with-meta anchor
                      {:component-did-mount
                       #(when (some? (get-in @props [id]))
                          ;; set up event for updating the ratom
                          (handle-dp-event dp c)
                          ;; set the date, either today or what was sent in
                          (if-let [d @c]
                            (.setDate dp (<-goog-date d) true)
                            (.setDate dp (<-goog-date (js/Date.)) true))
                          ;; render the datepicker
                          (.render dp (goog.dom/getElement (str "anchor-" id))))})


                    :else
                    (with-meta anchor
                      {:component-did-mount
                       #(when (some? (get-in @props [id]))
                          ;; set up event for updating the ratom
                          (let [e (goog.dom/getElement (str "anchor-" id))]
                            ;; set up event for updating the ratom
                            (handle-dp-event dp c)
                            ;; render the datepicker
                            (.render dp e)
                            ;; set the date
                            (if-let [d @c]
                              (do (.setDate dp (<-goog-date d) true)
                                  ;;(.setInputValue db )
                                  )
                              (.setDate dp (<-goog-date (js/Date.)) true))))}))
            data {:id id
                  :opts opts
                  :c c
                  :dp dp
                  :f f}]
        (swap! props assoc id data)
        data))))

(defmethod ez.field/field :datepicker [field form-options]
  (let [{:keys [id opts c dp f]} (get-props field form-options)]
    [:div.datepicker (merge {:id id :key (str "key-" id)} opts)
     [f id]
     ]))
