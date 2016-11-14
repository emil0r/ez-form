(defproject ez-form "0.7.1-SNAPSHOT"
  :description "Forms for the web"

  :url "https://github.com/emil0r/ez-form"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [vlad "3.3.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [reagent "0.6.0"]]

  :min-lein-version "2.0.0"

  :plugins [[lein-cljsbuild "1.1.4" :exclusions [[org.clojure/clojure]]]
            [lein-figwheel "0.5.8"]]

  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.1"]
                                  [binaryage/devtools "0.8.2"]
                                  [figwheel-sidecar "0.5.4-7"]
                                  [spyscope "0.1.5"]]

                   :source-paths ["src" "test"]

                   :injections [(require 'spyscope.core)
                                (use 'spyscope.repl)]

                   :repl-options {; for nREPL dev you really need to limit output
                                  :init (set! *print-length* 50)
                                  :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}

  :figwheel {:server-port 5123
             :nrepl-middleware ["cider.nrepl/cider-middleware"
                                "refactor-nrepl.middleware/wrap-refactor"
                                "cemerick.piggieback/wrap-cljs-repl"]}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :cljsbuild {:builds
              [{:id "test"
                :source-paths ["src" "test"]
                :compiler {:output-to "resources/test/compiled.js"
                           :optimizations :whitespace
                           :pretty-print true}}
               {:id "dev"
                :source-paths ["src" "test"]
                :figwheel true
                :compiler {:main ez_form.test.core
                           :source-map-timestamp true
                           :output-to "resources/public/js/compiled.js"
                           :output-dir "resources/public/js/out"
                           :asset-path "js/out"
                           :optimizations :none
                           :pretty-print true
                           :preloads [devtools.preload]}}]
              :test-commands
              {"form" ["phantomjs" "resources/test/test.js" "resources/test/test.html"]}})
