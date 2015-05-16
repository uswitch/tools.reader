(defproject org.clojure/tools.reader "0.9.3-SNAPSHOT"
  :description "A Clojure reader in Clojure"
  :parent [org.clojure/pom.contrib "0.1.2"]
  :url "https://github.com/clojure/tools.reader"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src/main/clojure" "src/main/clojurescript"]
  :test-paths ["src/test/clojure"]
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :dependencies [[org.clojure/clojure "1.7.0-master-SNAPSHOT"]
                 [org.clojure/clojurescript "0.0-3269"]]
  :plugins [[lein-cljsbuild "1.0.5"]]
  :profiles {:1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-master-SNAPSHOT"]]}
             :dev {:plugins [[com.cemerick/austin "0.1.6"]]}}
  :aliases {"test-all" ["with-profile" "test,1.4:test,1.5:test,1.6:test,1.7" "test"]
            "check-all" ["with-profile" "1.4:1.5:1.6:1.7" "check"]}
  :min-lein-version "2.0.0"
  :cljsbuild {:test-commands {"test" ["node" :runner "resources/test/tools.reader.test.js"]}
              :builds [{:id "dev"
                        :source-paths ["src/main/cljs"]
                        :compiler {:output-to "out/main.js"
                                   :output-dir "out"
                                   :optimizations :simple
                                   :pretty-print true}}
                       {:id "test"
                        :source-paths ["src/main/cljs" "src/test/cljs"]
                        :notify-command ["node" "resources/test/tools.reader.test.js"]
                        :compiler
                        {:output-to  "resources/test/tools.reader.test.js"
                         :source-map "resources/test/tools.reader.test.js.map"
                         :output-dir "resources/test/out"
                         :optimizations :simple}}]})
