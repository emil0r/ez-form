(ns build
  (:require
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as d]))

(def version "2025.04.27")
(def target "target")
(def class-dir (str target "/class-dir"))
(def lib 'ez-form/ez-form)
(def jar-file (format "target/%s.jar" (name lib)))


(defn jar-opts
  [opts]
  (let [basis    (b/create-basis opts)
        src-dirs (:paths basis)]
    (assoc opts
           :basis basis
           :target-dir class-dir
           :class-dir class-dir
           :lib lib
           :version version
           :src-dirs src-dirs
           :jar-file jar-file
           :pom-data [[:licenses
                       [:license
                        [:name "MIT"]
                        [:url "https://opensource.org/license/mit"]
                        [:distribution "repo"]]]])))

(defn clean
  [_]
  (b/delete {:path target}))

(defn jar
  "Build the library jar.
   Make sure to update the version number before building and deploying a new version."
  [opts]
  (let [opts (jar-opts opts)]
    (println (format "Writing pom file to %s" (:class-dir opts)))
    (b/write-pom opts)
    (println (format "Copying %s to %s" (:src-dirs opts) (:target-dir opts)))
    (b/copy-dir opts)
    (println (format "Writing jar file %s with contents from %s" (:jar-file opts) (:class-dir opts)))
    (b/jar opts)))


(defn install [_]
  (clean nil)
  (jar nil)
  (d/deploy {:installer :local
             :artifact jar-file
             :pom-file (b/pom-path {:lib lib :class-dir class-dir})}))

(defn deploy
  [_]
  (clean nil)
  (jar nil)
  (d/deploy {:installer :remote
             :artifact jar-file
             :pom-file (b/pom-path {:lib lib :class-dir class-dir})
             :sign-releases? true
             :sign-key-id (or (System/getenv "CLOJARS_GPG_ID")
                              (throw (RuntimeException. "CLOJARS_GPG_ID environment variable not set")))}))
