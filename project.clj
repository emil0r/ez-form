(defproject ez-form "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [vlad "3.3.0"]]
  :min-lein-version "2.0.0"
  :profiles {:dev {:plugins [[lein-midje "3.1.1"]]
                   :dependencies [[ring-mock "0.1.5"]
                                  [midje "1.6.3"]
                                  [spyscope "0.1.5"]]
                   :injections [(require 'spyscope.core)
                                (use 'spyscope.repl)]}})
