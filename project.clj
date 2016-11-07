(defproject ez-form "0.7.0-SNAPSHOT"
  :description "Forms for the web"

  :url "https://github.com/emil0r/ez-form"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [vlad "3.3.0"]]

  :min-lein-version "2.0.0"

  :plugins [[lein-cljsbuild "1.1.4"]]

  :profiles {:dev {:dependencies [[org.clojure/clojurescript "1.9.293"]
                                  [spyscope "0.1.5"]]
                   :injections [(require 'spyscope.core)
                                (use 'spyscope.repl)]}}

  :cljsbuild {:builds
              [{:id "test"
                :source-paths ["src" "test"]
                :compiler {:output-to "resources/test/compiled.js"
                           :optimizations :whitespace
                           :pretty-print true}}]
              :test-commands
              {"form" ["phantomjs" "resources/test/test.js" "resources/test/test.html"]}})
