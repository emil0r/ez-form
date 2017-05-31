(ns ez-form.flow
  (:require [clojure.string :as str]
            [clojure.walk :as walk]
            [clojure.zip :as zip]
            [ez-form.common :refer [get-field]]
            [ez-form.decorate :refer [decor decorate]]
            [ez-form.field :as ez.field]
            [ez-form.keywordize :refer [kw->string]]
            [ez-form.zipper :refer [zipper]]))


(defn marker [n]
  (flatten (map (fn [end]
                  [(keyword (str (kw->string n) "." end))
                   (keyword (str "$" (kw->string n) "." end))])
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

(defn correct-flowchart-for-template
  "Take a template flowchart, the current field, and add the id/name of the field to the marker so that the flow functions work on the flowchart"
  [flowchart field]
  (let [name (apply str (drop 1 (str (or (:id field) (:name field)))))]
    (walk/postwalk (fn [v]
                     (cond
                       (and (keyword? v)
                            (re-find #":\$.*" (str v)))
                       (let [v (apply str (drop 2 (str v)))]
                         (-> (str "$" name "." v)
                             keyword))

                       (and (keyword? v)
                            (re-find #":\?.*" (str v)))
                       (let [v (apply str (drop 2 (str v)))]
                         (-> (str "?" name "." v)
                             keyword))

                       :else
                       v)) flowchart)))

(defn get-markers
  "Get markers in the flowchart (ie, :$errors, :$field, etc)"
  [form]
  (->> form
       :fields
       (map (fn [{:keys [name id]}] (or id name)))
       (map marker)
       flatten))

(defn flow
  "Take a flowchart, a valid form, and output the content of the form into the flowchart"
  [flowchart form]
  (let [form (if (nil? (get-in form [:options :ez-form.core/as]))
               (assoc-in form [:options :ez-form.core/as] :flow)
               form)
        form-options (:options form)
        markers (get-markers form)
        output (loop [loc (zipper flowchart)]
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
                         (recur next-loc))))))]
    (if (= :flow (get-in form [:options :ez-form.core/as]))
      (decorate form (list :?ez-form.form-name output))
      (decorate form output))))
