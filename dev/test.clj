(ns test
  (:require [clojure.java.classpath :as classpath]
            [clojure.string :as str]
            [clojure.test]
            [clojure.tools.namespace.find :as namespace.find]))

(defn find-test-namespaces
  []
  (->> (namespace.find/find-namespaces
        (classpath/classpath-directories))
       (filter (fn [ns]
                 (str/ends-with? (str ns) "-test")))
       (remove #{'oiiku.actions-and-filters.actions-test
                 'oiiku.actions-and-filters.interface-test
                 'oiiku.asset-bundler.interface-test})))

(defn require-all-tests []
  (let [test-namespaces (find-test-namespaces)]
    (doseq [test-namespace test-namespaces]
      (require test-namespace))))

(defn run-all-tests []
  (apply clojure.test/run-tests (find-test-namespaces)))
