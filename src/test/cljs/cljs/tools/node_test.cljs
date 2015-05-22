(ns cljs.tools.node-test
  (:require [cljs.nodejs :as node]
            [cljs.tools.reader.impl.node :refer [NodePushbackReader]]
            [cljs.tools.reader :refer [read]]
            [cljs.tools.reader.reader-types :refer [source-logging-push-back-reader]]
            [cljs.test :as t :refer-macros [deftest is run-tests]]))

(deftest node-file-reader-test
  (let [fs (node/require "fs")
        path (node/require "path")
        reader-cljs (.resolve path js/__dirname "../../src/main/cljs/cljs/tools/reader.cljs")
        fs-read-stream (.createReadStream fs reader-cljs)
        node-reader (NodePushbackReader. fs-read-stream)
        source-logging-reader (source-logging-push-back-reader node-reader 1 reader-cljs)
        read? (atom false)]
    (.on fs-read-stream
         "readable"
         (fn []
           (when-not @read?
             (reset! read? true)
             (let [form (read source-logging-reader false nil)]
               (prn form)
               (prn (meta form))
               ))))))

(enable-console-print!)
(run-tests)
