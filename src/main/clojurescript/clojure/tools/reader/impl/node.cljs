(ns clojure.tools.reader.impl.node
  (:require
   [clojure.tools.reader :refer [read]]
   [clojure.tools.reader.reader-types
    :refer [Reader IPushbackReader read-char peek-char unread]]))

(enable-console-print!)

(def fs (js/require "fs"))

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

(def reader (.createReadStream fs "/tmp/test.clj"))

(def node-reader (NodePushbackReader. reader))

(defn do-read []
  (clojure.tools.reader/read node-reader false nil))
