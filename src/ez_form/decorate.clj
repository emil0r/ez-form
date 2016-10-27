(ns ez-form.decorate
  (:require [clojure.string :as str]
            [clojure.zip :as zip]))


(def ^:dynamic *materials* {:?wrapper {:class "error"}})

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

(let [m {:foo {:bar nil}}]
  (contains? (get-in m [:foo]) :bar))

(defn material [node]
  (if (and (keyword? node)
           (str/starts-with? (str node) ":?"))
    (keyword (str "?" (last (re-find #":\?.+\.(.*)" (str node)))))))

(def decor nil)
(defmulti decor
  "The decor function recognizes the decor in the form output and does the decoration"
  (fn [_ loc] (material (zip/node loc))))
(defmethod decor :?wrapper [form loc]
  (let [node (zip/node loc)
        base-material (material node)
        options (get-material form node base-material)]
    (if options
      (zip/replace loc options)
      (zip/replace loc nil))))
(defmethod decor :default [_ loc] loc)
