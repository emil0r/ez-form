(ns ez-form.flow
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [clojure.zip :as zip]
            [ez-form.zipper :refer [zipper]]
            [ez-form.field :as ez.field]))


(defn marker [n]
  (flatten (map (fn [end]
                  [(keyword (str (name n) "." end))
                   (keyword (str "$" (name n) "." end))])
                ["field" "errors" "label" "text" "help"])))

(defn field? [marker]
  (str/ends-with? (str marker) ".field"))

(defn errors? [marker]
  (str/ends-with? (str marker) ".errors"))

(defn label? [marker]
  (str/ends-with? (str marker) ".label"))

(defn text? [marker]
  (str/ends-with? (str marker) ".text"))

(defn help? [marker]
  (str/ends-with? (str marker) ".help"))

(defn correct-flowchart-for-template [flowchart field]
  (let [name (apply str (drop 1 (str (or (:id field) (:name field)))))]
    (walk/postwalk (fn [v]
                     (if (and (keyword? v)
                              (re-find #":\$.*" (str v)))
                       (let [v (apply str (drop 2 (str v)))]
                         (-> (str "$" name "." v)
                             keyword))
                       v)) flowchart)))

(defn get-field [form marker]
  (let [field (keyword (last (re-find #"^:(\$)?(.*)\..+" (str marker))))]
    (->> form
         :fields
         (filter (fn [{:keys [id name]}]
                   (or (= id field)
                       (= name field))))
         first)))

(defn get-markers [form]
  (->> form
       :fields
       (map (fn [{:keys [name id]}] (or id name)))
       (map marker)
       flatten))

(defn flow [flowchart form]
  (let [form-options (:options form)
        markers (get-markers form)]
    (loop [loc (zipper flowchart)]
      (let [next-loc (zip/next loc)]
        (if (zip/end? next-loc)
          (zip/root loc)
          (let [node (zip/node next-loc)
                marker? (some #(= node %) markers)]
            (if marker?
              (if-let [field (get-field form node)]
                (cond
                  (field? node) (recur (zip/replace next-loc (ez.field/field field form-options)))
                  (errors? node) (recur (zip/replace next-loc (ez.field/errors field)))
                  (label? node) (recur (zip/replace next-loc (ez.field/label field)))
                  (text? node) (recur (zip/replace next-loc (ez.field/text field)))
                  (help? node) (recur (zip/replace next-loc (ez.field/help field)))
                  :else (recur next-loc))
                (recur next-loc))
              (recur next-loc))))))))
