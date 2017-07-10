(ns ez-form.field.datepicker
  "Field for picking dates. Depends on goog.ui.DatePicker and subclasses"
  (:require [ez-form.common :as ez.common]
            [ez-form.field :as ez.field]
            [ez-form.i18n :refer [*t* *locale*]]
            [goog.events]
            [goog.events.EventType]
            [goog.dom]
            [goog.i18n.DateTimeFormat]
            [goog.i18n.DateTimeParse]
            [goog.ui.DatePicker]
            [goog.ui.DatePicker.Events]
            [goog.ui.InputDatePicker]
            [goog.ui.PopupDatePicker]
            [goog.ui.PopupBase.EventType]
            [reagent.core :as r]))


(defmulti goog<-date type)
(defmethod goog<-date :default [date]
  (if (some? date)
    ;; javascript... why oh why?!
    (goog.date.Date. (.getFullYear date) (.getMonth date) (.getDate date))
    nil))


(defn anchor-datepicker [{:keys [id c formatter]}]
  [:div.date-picker
   [:div
    (if-let [value @c]
      (.format formatter (js/Date. (+ (.getTime value) (* 1000 60 (.getTimezoneOffset value)))))
      (*t* *locale* ::pick-a-date))]
   [:div {:id (str "anchor-" id)}]])

(defn anchor-popup [{:keys [id c formatter]}]
  [:div.popup-date-picker
   [:div.date {:id (str "date-" id)}
    (if-let [value @c]
      (.format formatter (js/Date. (+ (.getTime value) (* 1000 60 (.getTimezoneOffset value)))))
      (*t* *locale* ::pick-a-date))]
   [:div {:id (str "anchor-" id)}]])

(defn anchor-input [{:keys [id]}]
  [:div.input-date-picker {:id (str "anchor-" id)}])

(defn- handle-dp-event [goog->date dp c]
  (goog.events/listen
   dp
   goog.ui.DatePicker.Events/CHANGE
   (fn [e]
     (if (.-date e)
       (reset! c (goog->date (.-date e)))
       (reset! c nil)))))

(defn- get-props [field form-options]
  (let [id (ez.common/get-first field :id :name)
        opts (ez.field/get-opts field [:class :name] form-options)
        c (:cursor field)
        goog->date (or (:goog->date field)
                       (fn [date]
                         (if (some? date)

                           (let [;; get the javascript date
                                 d (.-date date)
                                 ;; get the offset
                                 offset (-> date .-date .getTimezoneOffset)]
                             ;; add the offset after its been reversed
                             (.setMinutes d (+ (.getMinutes d) (* -1 offset)))
                             d))))
        {:keys [dp parser formatter]}
        (let [pattern (or (:pattern field)
                          "yyyy'-'MM'-'dd")
              formatter (goog.i18n.DateTimeFormat. pattern)
              parser (goog.i18n.DateTimeParse. pattern)]
          (cond (= :input (:mode field))
                {:dp (goog.ui.InputDatePicker. formatter parser)
                 :formatter formatter
                 :parser parser}

                (= :popup (:mode field))
                {:dp (goog.ui.PopupDatePicker.)
                 :formatter formatter
                 :parser parser}

                :else
                {:dp (goog.ui.DatePicker.)
                 :formatter formatter
                 :parser parser}))
        _ (when-let [b (get-in field [:props :date :show-fixed-num-weeks?])] (.setShowFixedNumWeeks dp b))
        _ (when-let [b (get-in field [:props :date :show-other-months?])] (.setShowOtherMonths dp b))
        _ (when-let [b (get-in field [:props :date :show-today?])] (.setShowToday dp b))
        _ (when-let [b (get-in field [:props :date :show-weekday-num?])] (.setShowWeekdayNum dp b))
        _ (when-let [b (get-in field [:props :date :show-weekday-names?])] (.setShowWeekdayNames dp b))
        _ (when-let [b (get-in field [:props :date :allow-none?])] (.setAllowNone dp b))
        _ (when-let [b (get-in field [:props :date :use-narrow-weekday-names?])] (.setUseNarrowWeekdayNames dp b))
        _ (when-let [b (get-in field [:props :date :use-allow-simple-navigation-menu?])] (.setUseAllowSimpleNavigationMenu dp b))
        _ (when-let [b (get-in field [:props :date :long-date-format?])] (.setLongDateFormat dp b))
        f (cond (= :input (:mode field))
                (with-meta anchor-input
                  {:component-did-mount
                   #(do ;; set up event for updating the ratom
                      (let [e (goog.dom/getElement (str "anchor-" id))]
                        ;; set up event for updating the ratom
                        (handle-dp-event goog->date dp c)
                        ;; render the datepicker
                        (.render dp e)
                        ;; set the date, do this after the date picker has been rendered
                        ;; if done before the rendering the date picked will not be set
                        ;; in the rendered widget
                        (if-let [d @c]
                          (.setDate dp (goog<-date d) true)
                          (.setDate dp (goog<-date (js/Date.)) true))

                        ;; set up feedback loop from the input
                        ;; NOTE: problem 1) setting the date closes the widget
                        ;;       problem 2) the strict parse can parse dates shorter
                        ;;                  than the input you would expect
                        #_(when-let [input (.getContentElement dp)]
                            (goog.events/listen input
                                                goog.events.EventType/KEYUP
                                                (fn [e]
                                                  (let [v (-> e .-target .-value)]
                                                    (try
                                                      (let [d (goog.date.Date.)
                                                            r (.strictParse parser v d)]
                                                        (println r)
                                                        (when (not= r 0)
                                                          (.setDate dp d false)))
                                                      (catch js/Error e
                                                        (println e)
                                                        ;; silently drop
                                                        ))))))))})

                (= :popup (:mode field))
                (with-meta anchor-popup
                  {:component-did-mount
                   #(do
                      ;; set up event for updating the ratom
                      (let [e (goog.dom/getElement (str "date-" id))]
                        ;; set up event for updating the ratom
                        (handle-dp-event goog->date dp c)
                        ;; render the datepicker
                        (.render dp)
                        ;; attach to the element
                        (.attach dp e)

                        ;; set the date if set
                        (goog.events/listen
                         dp
                         goog.ui.PopupBase.EventType/SHOW
                         (fn [e]
                           (if-let [d @c]
                             (.setDate dp (goog<-date d) true)
                             (.setDate dp (goog<-date (js/Date.)) true))))))})

                :else
                (with-meta anchor-datepicker
                  {:component-did-mount
                   #(do
                      ;; set up event for updating the ratom
                      (handle-dp-event goog->date dp c)

                      ;; render the datepicker
                      (.render dp (goog.dom/getElement (str "anchor-" id)))

                      ;; set the date, either today or what was sent in
                      (if-let [d @c]
                        (.setDate dp (goog<-date d) true)
                        (.setDate dp (goog<-date (js/Date.)) true)))}))]
    {:id id
     :opts opts
     :c c
     :dp dp
     :parser parser
     :formatter formatter
     :f f
     :goog->date goog->date}))

(defmethod ez.field/field :datepicker [field form-options]
  (let [{:keys [id opts c dp f formatter]} (get-props field form-options)]
    [:div.datepicker (merge {:id id :key (str "key-" id)} opts)
     [f {:id id :formatter formatter :c c}]]))
