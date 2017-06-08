(ns ez-form.field.timepicker
  "Field for picking time"
  (:require [ez-form.common :as ez.common]
            [ez-form.field :as ez.field]
            [ez-form.i18n :refer [*t* *locale*]]
            [goog.dom]
            [goog.events.KeyCodes :as KeyCodes]
            [reagent.core :as r]))


(def ^:private full-24hr (* 24 60 60))
(def ^:private noon (* 12 60 60))
(def ^:private ampm-time (* 12 60 60))
(def ^{:private true
       :doc "How do much we increase/decrease values by if the number is incremented/decremented by one?"}
  clock-inc-values {0 (* 10 60 60)
                    1 (* 60 60)
                    2 (* 10 60)
                    3 60
                    4 10
                    5 1})


;; -- helper functions for getting the number representing
;; the time at the given position of the clock
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
  [seconds {:keys [format]}]
  [(get-hours2 (= format :12hr) seconds)
   (get-hours1 (= format :12hr) seconds)
   (get-minutes2 seconds)
   (get-minutes1 seconds)
   (get-seconds2 seconds)
   (get-seconds1 seconds)])

(defn- add-half-day
  "Only for AM/PM"
  [c]
  (fn [e]
    (swap! c + noon)))
(defn- remove-half-day
  "Only for AM/PM"
  [c]
  (fn [e]
    (swap! c - noon)))

(defn- am-pm
  "Switch between AM/PM (only for the :12hr mode)"
  [c {:keys [format]}]
  (when (= format :12hr)
    (if (and (some? @c) (>= @c noon))
      [:div.ampm {:on-click (remove-half-day c)} "PM"]
      [:div.ampm {:on-click (add-half-day c)} "AM"])))

(defn- number-display
  "Display the number of the current position of the digital watch for the selected time"
  [up down position value c focus?]
  (let [klass (if (get @focus? position)
                {:class "focus"}
                nil)
        inc-value (get clock-inc-values position)]
    [:span.holder
     [:span.up {:on-click #(reset! c (mod (+ @c inc-value) full-24hr))} up]
     [:span.number (merge klass
                          {:key (str "time-" position)
                           :on-click #(reset! focus?
                                              (assoc [false false
                                                      false false
                                                      false false] position true))
                           }) value]
     [:span.down {:on-click #(reset! c (mod (- @c inc-value) full-24hr))} down]]))

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
                      (reset! c (mod (+ @c (* diff (get clock-inc-values position))) full-24hr))
                      (shift :right data))


                    (and (neg? diff)
                         (allowed-position? numbers k position props))
                    (do (reset! c (mod (+ @c (* diff (get clock-inc-values position))) full-24hr))
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
     [:span.reset {:on-click #(reset! c 0)} (*t* *locale* ::reset)]
     [number-display up down 0 h2 c focus?]
     [number-display up down 1 h1 c focus?]
     [number-display up down 2 m2 c focus?]
     [number-display up down 3 m1 c focus?]
     (if seconds? [number-display up down 4 s2 c focus?])
     (if seconds? [number-display up down 5 s1 c focus?])]))

(def ^:private -props (r/atom {}))

(defn- get-props [field form-options]
  (let [id (ez.common/get-first field :id :name)]
    (if (some? (get-in @-props [id]))
      (get @-props id)
      (let [opts (ez.field/get-opts field [:class :name] form-options)
            ;; number of seconds
            c (:cursor field)
            ;; the numbers displayed
            focus? (r/atom [false false false false false false])
            ;; -- props --
            ;; format: #{:24hr :12hr}
            ;; seconds?: #{true false}
            props (get-in field [:props :time])
            up (or (:up field) "▲")
            down (or (:down field) "▼")
            data {:id id
                  :opts opts
                  :c c
                  :focus? focus?
                  :props props
                  :up up
                  :down down}]
        (swap! -props assoc id data)
        data))))

(defmethod ez.field/field :timepicker [field form-options]
  (let [{:keys [up down id c focus? props opts]} (get-props field form-options)]
    [:div.timepicker (merge {:id id} opts)
     [time-field id up down c focus? props]
     [am-pm c props]]))
