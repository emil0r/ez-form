(ns ez-form.decorate
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [ez-form.zipper :refer [zipper]]))


(def ^:dynamic *materials* {:?wrapper {:class "error"}
                            :?text {:css {:class "text"}
                                    :wrapper :div}
                            :?help {:css {:class "text"}
                                    :wrapper :div}
                            :?error {:css {:class "text"}
                                     :wrapper :div}})

(defn add-decor [decor field]
  (let [-name (name (or (:id field) (:name field)))]
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

(defn- wrap-decor [form loc]
  (let [node (zip/node loc)
        to-wrap (zip/node (zip/next loc))
        base-material (material node)
        options (get-material form node base-material)]
    (if options
      (-> loc
          (zip/remove)
          (zip/replace [(:wrapper options) (:css options) to-wrap]))
      (zip/replace loc nil))))

(def decor nil)
(defmulti decor
  "The decor function recognizes the decor in the form output and does the decoration"
  (fn [_ loc] (material (zip/node loc))))
(defmethod decor :?text [form loc] (wrap-decor form loc))
(defmethod decor :?error [form loc] (wrap-decor form loc))
(defmethod decor :?help [form loc] (wrap-decor form loc))
(defmethod decor :?wrapper [form loc]
  (let [node (zip/node loc)
        base-material (material node)
        options (get-material form node base-material)]
    (if options
      (zip/replace loc options)
      (zip/remove loc))))
(defmethod decor :default [_ loc] loc)

(defn decorate [form output]
  (loop [loc (zipper output)]
    (let [next-loc (zip/next loc)]
      (if (zip/end? next-loc)
        (zip/root loc)
        (recur (decor form next-loc))))))