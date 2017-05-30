(ns ez-form.util)

(defn ->array [js-col]
  (-> (clj->js [])
      (.-slice)
      (.call js-col)
      (js->clj)))
