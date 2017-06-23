(ns ez-form.decorate
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [ez-form.common :refer [get-field get-first]]
            [ez-form.keywordize :refer [kw->string]]
            [ez-form.zipper :refer [zipper]]))


(def ^:dynamic *materials* {:?form-name {:class "hidden"}
                            :?wrapper {:class "error"}
                            :?text {:css {:class "text"}
                                    :wrapper :div}
                            :?help {:css {:class "help"}
                                    :wrapper :div}
                            :?error {:css {:class "error-message"}
                                     :wrapper :div}})

(defn add-decor [decor field]
  (let [-name (kw->string (or (:id field) (:name field)))]
    (keyword (str "?" -name "." (name decor)))))

(defn add-error-decor [field error]
  (if error
    (list (add-decor :error field) error)))

(defn add-help-decor [field]
  (if-let [help (:help field)]
    (list (add-decor :help field) help)))

(defn add-text-decor [field]
  (if-let [text (:text field)]
    (list (add-decor :text field) text)))

(defn add-label-decor [field]
  (let [label (get-first field true :label :name)
        id (get-first field :id :name)]
    [:label {:for id}
     (list (add-decor :label field) label)]))

(defn get-material
  "Get the material to decorate with for the node"
  [form node base-material]
  (cond
    ;; from static form options
    (contains? (get-in form [:options :decor]) node)
    (get-in form [:options :decor node])

    (contains? (get-in form [:options :decor]) base-material)
    (get-in form [:options :decor base-material])
    ;; eof static form options

    ;; from dynamic form options
    (contains? (get-in form [:options :data :decor]) node)
    (get-in form [:options :data :decor node])

    (contains? (get-in form [:options :data :decor]) base-material)
    (get-in form [:options :data :decor base-material])
    ;; eof dynamic form options

    ;; pick the default materials
    :else
    (or (get *materials* node)
        (get *materials* base-material))))

(defn material [node]
  (if (and (keyword? node)
           (str/starts-with? (str node) ":?"))
    (keyword (str "?" (last (re-find #":\?.+\.(.*)" (str node)))))))

#?(:cljs
   (defn display-error-messages [form field material-options errors]
     [:div (if (and errors
                    (not (nil? @(:cursor field))))
             {}
             {:style {:display "none"}})
      (map (fn [error]
             (let [display (if (fn? error)
                             (error form field)
                             error)
                   k (str "errors-" (kw->string (:name field)) "-" (str display))]
               [(:wrapper material-options) (merge {:key k} (:css material-options))
                display])) errors)]))
(defn- wrap-decor [form loc]
  (let [node (zip/node loc)
        to-wrap (zip/node (zip/next loc))
        base-material (material node)
        options (get-material form node base-material)]
    (if options
      (-> loc
          (zip/remove)
          (zip/replace [(:wrapper options) (:css options)
                        (if (fn? to-wrap)
                          (to-wrap form (get-field form node :wrapper))
                          to-wrap)]))
      (zip/replace loc nil))))

(defmulti decor
  "The decor function recognizes the decor in the form output and does the decoration"
  (fn [_ loc] (material (zip/node loc))))
(defmethod decor :?text [form loc] (wrap-decor form loc))
(defmethod decor :?help [form loc] (wrap-decor form loc))
#?(:clj  (defmethod decor :?error [form loc] (wrap-decor form loc)))
#?(:cljs (defmethod decor :?error [form loc]
           (let [node (zip/node loc)
                 field (get-field form node :error)
                 options (get-material form node (material node))
                 errors @(:errors field)]
             (zip/replace loc (display-error-messages form field options errors)))))
(defmethod decor :?label [form loc]
  (let [node (zip/node loc)
        next-node (zip/node (zip/next loc))]
    (-> loc
        (zip/remove)
        (zip/replace (if (fn? next-node)
                       (next-node form (get-field form node :wrapper))
                       next-node)))))
(defmethod decor :?wrapper [form loc]
  ;; clojurescript needs to decorate with a key for react's sake
  #?(:cljs (let [node (zip/node loc)
                 field (get-field form node :wrapper)
                 options (merge {:key (kw->string (or (:id field) (:name field)))}
                                (if (and (not (empty? @(:errors field)))
                                         (not (nil? @(:cursor field))))
                                  (get-material form node (material node))))]
             (zip/replace loc options)))
  ;; clojure can skip decorating with a key
  #?(:clj  (let [node (zip/node loc)
                 field (get-field form node :wrapper)]
             (if-not (empty? (:errors field))
               (let [base-material (material node)
                     options (get-material form node base-material)]
                 (if options
                   (zip/replace loc options)
                   (zip/remove loc)))
               (zip/remove loc)))))
(defmethod decor :?form-name [form loc]
  (let [form-name (get-in form [:options :name])
        as (get-in form [:options :ez-form.core/as])
        node (zip/node loc)
        base-material (material node)
        options (get-material form node base-material)]
    (cond
      (and form-name
           (= as :table))
      (zip/replace loc [:tr #?(:cljs (merge options {:key "__ez-form.form-name"}))
                            #?(:clj  options)
                        [:td
                         ;; react complains about :colspan
                         #?(:clj  {:colspan 2})
                         #?(:cljs {:colSpan 2})
                         [:input {:type "hidden" :name "__ez-form.form-name" :value form-name}]]])

      (and form-name
           (= as :list))
      (zip/replace loc [:input
                        #?(:cljs {:type "hidden" :name "__ez-form.form-name" :value form-name :key "__ez-form.form-name"})
                        #?(:clj  {:type "hidden" :name "__ez-form.form-name" :value form-name})])

      (and form-name
           (= as :paragraph))
      (zip/replace loc [:input
                        #?(:cljs {:type "hidden" :name "__ez-form.form-name" :value form-name :key "__ez-form.form-name"})
                        #?(:clj  {:type "hidden" :name "__ez-form.form-name" :value form-name})])

      (and form-name
           (= as :template))
      (zip/replace loc [:input
                        #?(:cljs {:type "hidden" :name "__ez-form.form-name" :value form-name :key "__ez-form.form-name"})
                        #?(:clj  {:type "hidden" :name "__ez-form.form-name" :value form-name})])

      (and form-name
           (= as :flow))
      (zip/replace loc [:input
                        #?(:cljs {:type "hidden" :name "__ez-form.form-name" :value form-name :key "__ez-form.form-name"})
                        #?(:clj  {:type "hidden" :name "__ez-form.form-name" :value form-name})])

      :else
      (zip/remove loc))))
(defmethod decor :default [_ loc] loc)

(defn decorate [form output]
  (loop [loc (zipper output)]
    (let [next-loc (zip/next loc)]
      (if (zip/end? next-loc)
        (zip/root loc)
        (recur (decor form next-loc))))))
