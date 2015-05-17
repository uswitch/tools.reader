(ns cljs.tools.reader.impl.node
  (:refer-clojure :exclude [read])
  (:require
   [cljs.tools.reader :refer [read]]
   [cljs.tools.reader.reader-types :refer
    [IndexingReader IPushbackReader Reader get-column-number
     get-file-name get-line-number read-char peek-char unread]]))

(enable-console-print!)

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
