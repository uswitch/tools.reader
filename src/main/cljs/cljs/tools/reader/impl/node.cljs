(ns cljs.tools.reader.impl.node
  (:refer-clojure :exclude [read])
  (:require
   [cljs.tools.reader :refer [read]]
   [cljs.tools.reader.reader-types
    :refer [Reader IPushbackReader read-char peek-char unread]]))

(enable-console-print!)

#_(def fs (js/require "fs"))

(deftype NodePushbackReader [readable]
  Reader
  (read-char [_]
    (str (.read readable 1)))
  (peek-char [_]
    (let [char (read-char readable)]
      (unread readable char)
      char))
  IPushbackReader
  (unread [_ char]
    (.unshift readable char)))

#_(def reader (.createReadStream fs "/tmp/test.clj"))

#_(def node-reader (NodePushbackReader. reader))

#_(defn do-read []
  (read node-reader false nil))
