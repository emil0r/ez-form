(ns ez-form.field.datetimepicker
  (:require [ez-form.common :as ez.common]
            [ez-form.field :as ez.field]
            [ez-form.field.datepicker :as datepicker]
            [ez-form.field.timepicker :as timepicker]
            [ez-form.i18n :refer [*t* *locale*]]
            [goog.events]
            [goog.events.EventType]
            [goog.dom]
            [goog.events.KeyCodes :as KeyCodes]
            [goog.i18n.DateTimeFormat]
            [goog.i18n.DateTimeParse]
            [goog.ui.DatePicker]
            [goog.ui.DatePicker.Events]
            [goog.ui.InputDatePicker]
            [goog.ui.PopupDatePicker]
            [goog.ui.PopupBase.EventType]
            [reagent.core :as r]))


;; -- time part --

(def ^:private full-24hr (* 24 60 60))
(def ^:private noon (* 12 60 60))
(def ^:private ampm-time (* 12 60 60))
(def ^{:private true
       :doc "How do much we increase/decrease values by if the number is incremented/decremented by one?"}
  clock-inc-values {0 (* 10 60 60 1000)
                    1 (* 60 60 1000)
                    2 (* 10 60 1000)
                    3 (* 60 1000)
                    4 (* 10 1000)
                    5 1000})


(defn- get-seconds1 [seconds]
  (mod seconds 10))
(defn- get-seconds2 [seconds]
  (int (/ (mod seconds 60) 10)))

(defn- get-minutes1 [seconds]
  (mod (int (/ seconds 60)) 10))
(defn- get-minutes2 [seconds]
  (int (/ (mod (int (/ seconds 60)) 60) 10)))

(defn- get-hours1 [ampm? seconds]
  (if (true? ampm?)
    (if (>= seconds noon)
      (mod (int (/ (- seconds noon) 3600)) 10)
      (mod (int (/ seconds 3600)) 10))
    (mod (int (/ seconds 3600)) 10)))
(defn- get-hours2 [ampm? seconds]
  (let [v (int (/ (int (/ (if (and ampm? (>= seconds ampm-time))
                            (- seconds ampm-time)
                            seconds) 3600)) 10))]
    v))

(defn- get-numbers
  "Given x seconds, return the numbers repesenting the time"
  [date {:keys [format]}]
  (if date
    (let [;; new shadow date created and give back the timezone offset
          date-shadow (js/Date. (+ (.getTime date) (* 1000 60 (.getTimezoneOffset date))))
          hours (* 3600 (mod (.getHours date-shadow) 24))
          minutes (* 60 (.getMinutes date-shadow))
          seconds (.getSeconds date-shadow)]
      [(get-hours2 (= format :12hr) hours)
       (get-hours1 (= format :12hr) hours)
       (get-minutes2 minutes)
       (get-minutes1 minutes)
       (get-seconds2 seconds)
       (get-seconds1 seconds)])
    [0 0 0 0 0 0]))

(defn- add-half-day
  "Only for AM/PM"
  [c]
  (fn [e]
    (when @c
      (reset! c (js/Date. (+ (.getTime @c) (* noon 1000)))))))
(defn- remove-half-day
  "Only for AM/PM"
  [c]
  (fn [e]
    (when @c
      (reset! c (js/Date. (+ (.getTime @c) (* -1000 noon)))))))

(defn- am-pm
  "Switch between AM/PM (only for the :12hr mode)"
  [c {:keys [format]}]
  (when (= format :12hr)
    (let [date @c]
      (if (and (some? date) (>= (+ (* 3600 (.getHours date)) (* 60 (.getMinutes date)) (.getSeconds date))
                                noon))
        [:div.ampm {:on-click (remove-half-day c)} "PM"]
        [:div.ampm {:on-click (add-half-day c)} "AM"]))))

(defn- number-display
  "Display the number of the current position of the digital watch for the selected time"
  [up down position value c focus?]
  (let [klass (if (get @focus? position)
                {:class "focus"}
                nil)
        inc-value (get clock-inc-values position)]
    [:span.holder
     [:span.up {:on-click #(when-let [date @c]
                             (reset! c (js/Date. (+ (.getTime date) inc-value))))} up]
     [:span.number (merge klass
                          {:key (str "time-" position)
                           :on-click #(reset! focus?
                                              (assoc [false false
                                                      false false
                                                      false false] position true))
                           }) value]
     [:span.down {:on-click #(when-let [date @c]
                               (reset! c (js/Date. (- (.getTime date) inc-value))))} down]]))

(defn- get-position [focus? {:keys [seconds?]}]
  (let [max-position (if-not seconds? 3 5)]
    (reduce (fn [out v]
              (cond
                (true? v)
                (reduced out)

                (< out max-position)
                (inc out)

                :else
                (reduced nil)))
            0 @focus?)))

(defn- shift
  "Shift focus left or right for the visual numbers"
  [way {:keys [focus? props]}]
  (let [focus @focus?
        max-position (if-not (:seconds? props)
                       3
                       5)
        position (get-position focus? props)]
    (when position
      (reset! focus?
              (cond
                ;; edge of the array to the left
                (and (= way :left)
                     (<= position 0))
                [false false false false false false]

                ;; edge of the array to the right
                (and (= way :right)
                     (>= position max-position))
                [false false false false false false]

                (= way :left)
                (-> focus
                    (assoc position false)
                    (assoc (dec position) true))

                (= way :right)
                (-> focus
                    (assoc position false)
                    (assoc (inc position) true))

                :else
                focus)))))

(defn- allowed-position? [numbers k position props]
  (let [ampm? (= (:format props) :12hr)]
    (or
     ;; 3 and 5 can go all the way up to 9
     (and (#{3 5} position))
     ;; 2 and 4 can only go up to a maximum of 5
     (and (#{2 4} position)
          (<= k 5))
     ;; 1 can go up to 9 IF it's a 24hr format
     (and (= 1 position)
          (false? ampm?))
     ;; allow up to 9 if number 0 is 0 and 12hr format
     (and (= 1 position)
          (true? ampm?)
          (zero? (get numbers 0)))
     ;; allow up to 2 if number 0 above 0 and 12hr format
     (and (= 1 position)
          (true? ampm?)
          (not (zero? (get numbers 0)))
          (<= k 2))
     ;; allow up to 9 if number 0 < 2 and 24hr format
     (and (= 1 position)
          (false? ampm?)
          (< 2 (get numbers 0)))
     ;; allow up to 4 if number 0 = 2 and 24hr format
     (and (= 1 position)
          (false? ampm?)
          (= 2 (get numbers 0))
          (<= k 4))

     (and (= 0 position)
          (true? ampm?)
          (<= k 1))

     (and (= 0 position)
          (false? ampm?)
          (<= k 2)))))

(defn- handle-keydown-event
  "Increase/decrease time"
  [{:keys [focus? c props] :as data}]
  (fn [e]
    (let [key (or (.-which e) (.-keyCode e))
          codes {0 [KeyCodes/ZERO KeyCodes/NUM_ZERO]
                 1 [KeyCodes/ONE KeyCodes/NUM_ONE]
                 2 [KeyCodes/NUM_TWO KeyCodes/TWO]
                 3 [KeyCodes/NUM_THREE KeyCodes/THREE]
                 4 [KeyCodes/NUM_FOUR KeyCodes/FOUR]
                 5 [KeyCodes/NUM_FIVE KeyCodes/FIVE]
                 6 [KeyCodes/NUM_SIX KeyCodes/SIX]
                 7 [KeyCodes/NUM_SEVEN KeyCodes/SEVEN]
                 8 [KeyCodes/NUM_EIGHT KeyCodes/EIGHT]
                 9 [KeyCodes/NUM_NINE KeyCodes/NINE]
                 :up [KeyCodes/UP KeyCodes/PAGE_UP]
                 :down [KeyCodes/DOWN KeyCodes/PAGE_DOWN]
                 :left [KeyCodes/LEFT]
                 :right [KeyCodes/RIGHT]
                 :enter [KeyCodes/ENTER]}]
      (doseq [[k codes] codes]
        (when (some #(= % key) codes)
          (cond
            (= k :enter)
            (reset! focus? [false false false false false false])

            ;; shift the focus if we press left or right
            (or (= :left k) (= :right k))
            (shift k data)

            (= k :up)
            (when-let [position (get-position focus? props)]
              (reset! c (mod (+ @c (get clock-inc-values position)) full-24hr)))

            (= k :down)
            (when-let [position (get-position focus? props)]
              (reset! c (mod (- @c (get clock-inc-values position)) full-24hr)))

            (some #(= % k) [0 1 2 3 4 5 6 7 8 9])
            (let [numbers (get-numbers @c props)
                  position (get-position focus? props)
                  number (get numbers position)
                  diff (- k number)]
              (cond (zero? diff)
                    (shift :right data)

                    (and (pos? diff)
                         (allowed-position? numbers k position props))
                    (do
                      (reset! c (js/Date. (+ (.getTime @c) (* diff (get clock-inc-values position)))))
                      (shift :right data))


                    (and (neg? diff)
                         (allowed-position? numbers k position props))
                    (do (reset! c (js/Date. (+ (.getTime @c) (* diff (get clock-inc-values position)))))
                        (shift :right data))

                    :else
                    nil))

            ;; nothing
            :else
            nil))))

    (set! (-> e .-target .-value) "")))
(defn- hidden-input [{:keys [id focus? c] :as data}]
  (let [id (str id "-hidden-input")]
    (when-not (every? false? @focus?)
      (.focus (.getElementById js/document id)))
    [:div {:style {:width 0 :overflow "hidden"}}
     [:input {:id id
              :type :number
              :on-key-down (handle-keydown-event data)}]]))

(defn- time-field [id up down c focus? {:keys [format seconds?] :as props}]
  (let [seconds (or @c 0)
        [h2 h1 m2 m1 s2 s1] (get-numbers @c props)]
    [:div.time
     [hidden-input {:id id :focus? focus? :c c :props props}]
     [:span.reset {:on-click #(when-let [date @c]
                                (.setHours date 0)
                                (.setMinutes date 0)
                                (.setSeconds date 0)
                                (reset! c (js/Date. (+ (.getTime date) (* -1000 60 (.getTimezoneOffset date))))))} (*t* *locale* ::reset)]
     [number-display up down 0 h2 c focus?]
     [number-display up down 1 h1 c focus?]
     [number-display up down 2 m2 c focus?]
     [number-display up down 3 m1 c focus?]
     (if seconds? [number-display up down 4 s2 c focus?])
     (if seconds? [number-display up down 5 s1 c focus?])]))



;; --- date part ---

(defmulti goog<-datetime type)
(defmethod goog<-datetime :default [date]
  (if (some? date)
    ;; javascript... why oh why?!
    (goog.date.Date. date ;;(.getFullYear date) (.getMonth date) (.getDate date)
     )
    nil))


(defn- handle-dp-event [goog->datetime dp c]
  (goog.events/listen
   dp
   goog.ui.DatePicker.Events/CHANGE
   (fn [e]
     (let [date @c
           new-date (goog->datetime (.-date e) date)]
       (when (or (nil? new-date)
                 (not= js/Date (type date))
                 (not= (.getTime date) (.getTime new-date)))
         (reset! c new-date))))))

(defn- get-props [field form-options]
  (let [id (ez.common/get-first field :id :name)
        opts (ez.field/get-opts field [:class :name] form-options)
        c (:cursor field)
        goog->datetime (or (:goog->datetime field)
                           (fn [goog-date js-date]
                             (if (some? goog-date)
                               (let [offset (if js-date
                                              (-> goog-date .-date .getTimezoneOffset)
                                              0)
                                     ;; get the javascript date
                                     d (js/Date. (+ (.getTime (.-date goog-date))
                                                    (* 3600 1000 (if js-date
                                                                   (.getHours js-date)
                                                                   0))
                                                    (* 60 1000 (if js-date
                                                                 (.getMinutes js-date)
                                                                 0))
                                                    (* 1000 (if js-date
                                                              (.getSeconds js-date)
                                                              0))
                                                    ;; - remove the offset
                                                    ;;   cljs doesn't seem
                                                    ;;   too happy with js/Date
                                                    ;;   and #inst conversion
                                                    ;;   that involves timezones
                                                    ;; - in addition, most of the
                                                    ;;   time you're not super
                                                    ;;   interested in a timezone
                                                    ;;   when setting a time in
                                                    ;;   a datetime picker. so
                                                    ;;   we aim for a compromise
                                                    (* -1000 60 offset)))]
                                 d)
                               nil)))
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
                (with-meta datepicker/anchor-input
                  {:component-did-mount
                   #(do
                      ;; set up event for updating the ratom
                      (let [e (goog.dom/getElement (str "anchor-" id))]
                        ;; set up event for updating the ratom
                        (handle-dp-event goog->datetime dp c)
                        ;; render the datepicker
                        (.render dp e)
                        ;; set the date, do this after the date picker has been rendered
                        ;; if done before the rendering the date picked will not be set
                        ;; in the rendered widget
                        (if-let [d @c]
                          (.setDate dp (goog<-datetime d) true)
                          (.setDate dp (goog<-datetime (js/Date.)) true))))})

                (= :popup (:mode field))
                (with-meta datepicker/anchor-popup
                  {:component-did-mount
                   #(do
                      ;; set up event for updating the ratom
                      (let [e (goog.dom/getElement (str "date-" id))]
                        ;; set up event for updating the ratom
                        (handle-dp-event goog->datetime dp c)
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
                             (.setDate dp (goog<-datetime d) true)
                             (.setDate dp (goog<-datetime (js/Date.)) true))))))})

                :else
                (with-meta datepicker/anchor-datepicker
                  {:component-did-mount
                   #(do
                      ;; set up event for updating the ratom
                      (handle-dp-event goog->datetime dp c)

                      ;; render the datepicker
                      (.render dp (goog.dom/getElement (str "anchor-" id)))

                      ;; set the date, either today or what was sent in
                      (if-let [d @c]
                        (.setDate dp (goog<-datetime d) true)
                        (.setDate dp (goog<-datetime (js/Date.)) true)))}))]
    {:id id
     :opts opts
     :focus? (r/atom [false false false false false false])
     :c c
     :dp dp
     :up (or (:up field) "▲")
     :down (or (:down field) "▼")
     :parser parser
     :formatter formatter
     :time-props (get-in field [:props :time])
     :f f
     :goog->datetime goog->datetime}))

(defmethod ez.field/field :datetimepicker [field form-options]
  (let [{:keys [id down up time-props focus? opts c dp f formatter]} (get-props field form-options)]
    [:div.datetimepicker (merge {:id id :key (str "key-" id)} opts)
     [f {:id id :formatter formatter :c c}]
     [time-field id up down c focus? time-props]
     [am-pm c time-props]]))
