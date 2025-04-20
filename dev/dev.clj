(ns dev
  (:require [clojure.string :as str]
            [ez-form.core :as ezform :refer [defform]]
            [ez-form-test.forms :refer [replicant-form]]
            [hiccup.page :refer [doctype]]
            [hiccup2.core :as h]
            [org.httpkit.server :as server]
            [playback.core]
            [ring.middleware.anti-forgery :refer [*anti-forgery-token*
                                                  wrap-anti-forgery]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
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
        sl-form (shoelace-form {} (:params request))
        r-form  (replicant-form {} (:params request))]
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
                           :src  "https://cdn.jsdelivr.net/npm/@shoelace-style/shoelace@2.20.1/cdn/shoelace.js"}]
                 [:script {:type "text/javascript"
                           :src  "/js/main.js"}]]
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
                  [:input {:type :submit :value "Sumbit"}]]

                 [:h1 "Test replicant-form (copy of shoelace-form)"]
                 [:p "This will be rendered by hiccup on the backend and replicant on the frontend"]
                 [:div#replicant-app
                  [:form {:method :post}
                   (ezform/as-table r-form)
                   [:input {:type :submit :value "Submit"}]]]
                 [:script
                  {:type "text/javascript"}
                  (h/raw (str "ez_form_test.core.init('" *anti-forgery-token*
                              "','"
                              (pr-str (:params request))
                              "');"))]]))}))


(defonce server-instance (atom nil))

(defn start []
  (when @server-instance
    (@server-instance))
  (let [app (-> #'handler
                (wrap-anti-forgery)
                (wrap-session)
                (wrap-keyword-params)
                (wrap-params)
                (wrap-resource "public"))]
    (reset! server-instance (server/run-server app {:port 5555})))
  (println "dev server started on port 5555"))

(comment

  (start)

  (do
    ;; Ensure all tests are imported
    ((requiring-resolve 'test/require-all-tests))
    ;; Run all tests
    ((requiring-resolve 'test/run-all-tests)))

  )
