(ns ez-form.zipper
  (:require [clojure.zip :as zip]))


(defmulti branch? type)
(defmethod branch? :default [_] false)
#?(:clj  (defmethod branch? clojure.lang.IPersistentVector [v] true))
#?(:clj  (defmethod branch? clojure.lang.IPersistentMap [m] true))
#?(:clj  (defmethod branch? clojure.lang.IPersistentList [l] true))
#?(:clj  (defmethod branch? clojure.lang.ISeq [s] true))
#?(:cljs (defmethod branch? cljs.core/IVector [v] true))
#?(:cljs (defmethod branch? cljs.core/IMap [m] true))
#?(:cljs (defmethod branch? cljs.core/IList [l] true))
#?(:cljs (defmethod branch? cljs.core/ISeq [s] true))

(defmulti seq-children type)
#?(:clj  (defmethod seq-children clojure.lang.IPersistentVector [v] v))
#?(:clj  (defmethod seq-children clojure.lang.IPersistentMap [m] (mapv identity m)))
#?(:clj  (defmethod seq-children clojure.lang.IPersistentList [l] l))
#?(:clj  (defmethod seq-children clojure.lang.ISeq [s] s))
#?(:cljs (defmethod seq-children cljs.core/IVector [v] v))
#?(:cljs (defmethod seq-children cljs.core/IMap [m] (mapv identity m)))
#?(:cljs (defmethod seq-children cljs.core/IList [l] l))
#?(:cljs (defmethod seq-children cljs.core/ISeq [s] s))

(defmulti make-node (fn [node children] (type node)))
#?(:clj  (defmethod make-node clojure.lang.IPersistentVector [v children] (vec children)))
#?(:clj  (defmethod make-node clojure.lang.IPersistentMap [m children] (into {} children)))
#?(:clj  (defmethod make-node clojure.lang.IPersistentList [_ children] children))
#?(:clj  (defmethod make-node clojure.lang.ISeq [node children] (apply list children)))
#?(:cljs (defmethod make-node cljs.core/IVector [v children] (vec children)))
#?(:cljs (defmethod make-node cljs.core/IMap [m children] (into {} children)))
#?(:cljs (defmethod make-node cljs.core/IList [_ children] children))
#?(:cljs (defmethod make-node cljs.core/ISeq [node children] (apply list children)))


(defn zipper [node]
  (zip/zipper branch? seq-children make-node node))

#?(:clj  (prefer-method make-node clojure.lang.IPersistentList clojure.lang.ISeq))
#?(:clj  (prefer-method branch? clojure.lang.IPersistentList clojure.lang.ISeq))
#?(:clj  (prefer-method seq-children clojure.lang.IPersistentList clojure.lang.ISeq))
#?(:cljs (prefer-method make-node cljs.core/IList cljs.core/ISeq))
#?(:cljs (prefer-method branch? cljs.core/IList cljs.core/ISeq))
#?(:cljs (prefer-method seq-children cljs.core/IList cljs.core/ISeq))
