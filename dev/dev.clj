(ns dev
  (:require [clojure.string :as str]
            [ez-form.core :as ezform :refer [defform]]
            [ez-form.field :as field]
            [hiccup.page :refer [doctype]]
            [hiccup2.core :as h]
            [org.httpkit.server :as server]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]))


(defform signup-form
  {}
  [{:name       ::username
    :validation [{:spec      #(and
                               (string? %)
                               (not (str/blank? %)))
                  :error-msg [:div.error "Username cannot be foobar"]}]
    :type       :text}
   {:name       ::email
    :type       :email
    :validation [{:spec      #(and
                               (string? %)
                               (str/includes? % "@"))
                  :error-msg [:div.error "Email must have an @ character"]}]}])


(defn sl-input-color-picker [{:keys [type attributes]}]
  (let [type* (name type)]
    [type (merge attributes
                 {:type (subs type* 3 (count type*))})]))

(defn sl-input [{:keys [type attributes]}]
  (let [type* (name type)]
    [:sl-input (merge attributes
                      {:type (subs type* 9 (count type*))})]))

(defform shoelace-form
  {:extra-fields {:sl-color-picker sl-input-color-picker
                  :sl-input-email  sl-input
                  :sl-input-number sl-input
                  :sl-input-date   sl-input}}
  [{:name       ::color
    :type       :sl-color-picker
    :validation [{:spec      #(not (str/blank? %))
                  :error-msg [:div.error "Color must be picked"]}]}
   {:name       ::email
    :type       :sl-input-email
    :validation [{:spec      #(and
                               (string? %)
                               (str/includes? % "@"))
                  :error-msg [:div.error "Email must have an @ character"]}]}
   {:name ::number
    :type :sl-input-number}
   {:name ::date
    :type :sl-input-date}])

(defn handler [request]
  #_(def request request)
  (let [form    (signup-form {} (:params request))
        sl-form (shoelace-form {} (:params request))]
    {:status 200
     :body   (str
              (h/html
                  (:html5 doctype)
                [:head
                 [:meta {:charset "UTF-8"}]
                 [:title "ez-form test page"]
                 [:style {:type "text/css"}
                  ".error {color: red;}"]
                 [:link {:rel  "stylesheet"
                         :href "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.20.1/cdn/themes/light.css"}]
                 [:script {:type "module"
                           :src  "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.20.1/cdn/shoelace.js"}]]
                [:body
                 [:h1 "Test signup-form"]
                 (when (ezform/valid? form)
                   (list [:h3 "Data from form"]
                         [:pre (pr-str (ezform/fields->map form))]))
                 [:form {:method :post}
                  (ezform/as-table form)
                  [:input {:type :submit :value "Sumbit"}]]

                 [:h1 "Test shoelace-form"]
                 (when (ezform/valid? sl-form)
                   (list [:h3 "Data from shoelace-form"]
                         [:pre (pr-str (ezform/fields->map sl-form))]))
                 [:form {:method :post}
                  (ezform/as-table sl-form)
                  [:input {:type :submit :value "Sumbit"}]]]))}))


(defonce server-instance (atom nil))

(defn start []
  (when @server-instance
    (@server-instance))
  (let [app (-> #'handler
                (wrap-anti-forgery)
                (wrap-session)
                (wrap-keyword-params)
                (wrap-params))]
    (reset! server-instance (server/run-server app {:port 5555})))
  (println "dev server started on port 5555"))

(comment

  (start)

  )
