(ns ez-form.keywordize
  "Adapted from ring.middleware.keyword-params")

(defn keyword-syntax?
  "Allow for keywords that are namespaced as well"
  [s]
  (first (re-matches #"(([\p{L}0-9*+!_?-]*\.)*([\p{L}0-9*+!_?-]*/)*)?([\p{L}*+!_?-][\p{L}0-9*+!_?-]*)" s)))

(defn keyify [target]
  (cond
    (map? target)
      (into {}
        (for [[k v] target]
          [(if (and (string? k) (keyword-syntax? k))
             (keyword k)
             k)
           (keyify v)]))
    (vector? target)
      (vec (map keyify target))
    :else
      target))

(defn kw->string [k]
  (if (keyword? k)
    (.substring (str k) 1)
    k))
