(ns dev
  (:require [clojure.string :as str]
            [ez-form.core :as ezform :refer [defform]]
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

(defn handler [request]
  (def request request)
  (let [form (signup-form {} (:params request))]
    {:status 200
     :body   (str
              (h/html
                  (:html5 doctype)
                  [:head
                   [:meta {:charset "UTF-8"}]
                   [:title "ez-form test page"]
                   [:style {:type "text/css"}
                    ".error {color: red;}"]]
                  [:body
                   [:h1 "Test signup-form"]
                   [:form {:method :post}
                    (ezform/as-table form)
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
