(ns cljs.tools.node-test
  (:require [cljs.nodejs :as node]
            [cljs.tools.reader :refer [read]]
            [cljs.tools.reader.reader-types :as r]
            [cljs.test :as t :refer-macros [deftest is run-tests]]))

(deftest node-file-reader-test
  (let [fs (node/require "fs")
        path (node/require "path")
        reader-cljs (.resolve path js/__dirname "../../src/main/cljs/cljs/tools/reader.cljs")
        fs-read-stream (.createReadStream fs reader-cljs)
        node-reader (r/node-readable-push-back-reader fs-read-stream)
        source-logging-reader (r/source-logging-push-back-reader node-reader 1 reader-cljs)
        read? (atom false)]
    (.on fs-read-stream
         "readable"
         (fn []
           (when-not @read?
             (reset! read? true)
             (is (= (read source-logging-reader false nil)
                    '(ns ^{:doc "A clojure reader in clojure"
                           :author "Bronsa"}
                       cljs.tools.reader
                       (:refer-clojure :exclude [read read-line read-string char
                                                 default-data-readers *default-data-reader-fn*
                                                 *read-eval* *data-readers* *suppress-read*])
                       (:require
                        [cljs.tools.reader.reader-types :refer
                         [read-char reader-error unread peek-char indexing-reader?
                          get-line-number get-column-number get-file-name
                          string-push-back-reader log-source]]
                        [cljs.tools.reader.impl.utils :refer
                         [char ex-info? whitespace? numeric? desugar-meta next-id thread-bound?]]
                        [cljs.tools.reader.impl.commons :refer
                         [number-literal? read-past match-number parse-symbol read-comment throwing-reader]]
                        [clojure.string :as string]
                        [goog.array :as ga]
                        [goog.string :as gs])
                       (:import
                        [goog.string StringBuffer]))))
             (let [form (read source-logging-reader false nil)]
               (is (= form
                      '(declare read* macros dispatch-macros
                                *read-eval* *data-readers*
                                *default-data-reader-fn* *suppress-read*
                                default-data-readers)))
               (is (= (meta (second form))
                      {:source "^:private read*"
                       :file "/Users/Andrew.Mcveigh/Projects/tools.reader/src/main/cljs/cljs/tools/reader.cljs"
                       :line 34
                       :column 20
                       :end-line 34
                       :end-column 25
                       :private true})))
             (is (= (read source-logging-reader false nil)
                    '(defrecord UnresolvedKeyword [namespace name]))))))))
