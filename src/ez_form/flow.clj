(ns ez-form.flow
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [ez-form.field :as ez.field]))


(defn marker [n]
  (map #(keyword (str (name n) "." %)) ["field" "errors" "label" "text" "help"]))

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

(defn get-field [form marker]
  (let [field (keyword (last (re-find #"^:(.*)\..+" (str marker))))]
    (->> form
         :fields
         (filter (fn [{:keys [name]}]
                   (= name field)))
         first)))

(defn get-markers [form]
  (->> form
       :fields
       (map :name)
       (map marker)
       flatten))

(defn flow [flowchart form]
  (let [form-options (:options form)
        markers (get-markers form)]
    (loop [loc (zip/vector-zip flowchart)]
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
