(ns ez-form.field.timepicker
  "Field for picking time"
  (:require [ez-form.common :as ez.common]
            [ez-form.field :as ez.field]
            [ez-form.i18n :refer [*t* *locale*]]
            [reagent.core :as r]))


(def ^:private full-24hr (* 24 60 60))
(def ^:private noon (* 12 60 60))
(def ^:private ampm-time (* 13 60 60))

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
    (if (>= seconds ampm-time)
      (mod (int (/ seconds 3600)) 12)
      (mod (int (/ seconds 3600)) 10))
    (mod (int (/ seconds 3600)) 10)))
(defn- get-hours2 [ampm? seconds]
  (let [v (int (/ (int (/ (if (and ampm? (>= seconds ampm-time))
                            (- seconds ampm-time)
                            seconds) 3600)) 10))]
    v))

(defn- pretty-print-time [value focus? {:keys [format seconds?]}]
  (let [value (or value 0)]
    (list value focus?)))

(defn- add-half-day [c]
  (fn [e]
    (swap! c + noon)))
(defn- remove-half-day [c]
  (fn [e]
    (swap! c - noon)))

(defn- am-pm [c {:keys [format]}]
  (when (= format :12hr)
    (if (and (some? @c) (>= @c noon))
      [:div.ampm {:on-click (remove-half-day c)} "PM"]
      [:div.ampm {:on-click (add-half-day c)} "AM"])))

(defn- number-display [position inc-value value c]
  [:span {:key (str "time-" position)
          :on-click #(reset! c (mod (+ @c inc-value) full-24hr))} value])

(defn- time-field [c focus? {:keys [format seconds?] :as props}]
  (let [seconds (or @c 0)
        [h2 h1 m2 m1 s2 s1] [(get-hours2 (= format :12hr) seconds)
                             (get-hours1 (= format :12hr) seconds)
                             (get-minutes2 seconds)
                             (get-minutes1 seconds)
                             (get-seconds2 seconds)
                             (get-seconds1 seconds)]]
    [:div.time
     [number-display 0 (* 12 60 60) h2 c]
     [number-display 0 (* 60 60) h1 c]
     [number-display 0 (* 10 60) m2 c]
     [number-display 0 60 m1 c]
     (if seconds? [number-display 0 10 s2 c])
     (if seconds? [number-display 0 1 s1 c])]))

(defmethod ez.field/field :timepicker [field form-options]
  (let [id (ez.common/get-first field :id :name)
        opts (ez.field/get-opts field [:class :name] form-options)
        ;; number of seconds
        c (:cursor field)
        ;; the numbers displayed
        focus? (r/atom [false false false false false false])
        ;; -- props --
        ;; format: #{:24hr :12hr}
        ;; seconds?: #{true false}
        props (get-in field [:props :time])]
    [:div.timepicker (merge {:id id} opts)
     [time-field c focus? props]
     [am-pm c props]]))
