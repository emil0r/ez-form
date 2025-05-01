(ns ez-form-test.core
  (:require [clojure.edn :as edn]
            [ez-form-test.forms :refer [replicant-form]]
            [ez-form.core :as ezform]
            [replicant.dom :as r]))


(defn render [anti-forgery-token params]
  (r/render
   (js/document.getElementById "replicant-app")
   [:form {:method :post}
    (ezform/as-table (replicant-form {} params {:anti-forgery-token anti-forgery-token}))
    [:input {:type :submit :value "Submit"}]]))

(defonce aft (atom nil))

(defn ^:export init [anti-forgery-token params]
  (reset! aft anti-forgery-token)
  (println ::init)
  (let [params* (edn/read-string params)]
    (println ::params params*)
    (render anti-forgery-token params*)))

(defn start []
  (init @aft nil))

(defn stop [])
