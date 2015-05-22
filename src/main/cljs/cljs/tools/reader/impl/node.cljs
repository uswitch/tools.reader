(ns cljs.tools.reader.impl.node
  (:refer-clojure :exclude [read])
  (:require
   [cljs.tools.reader :refer [read]]
   [cljs.tools.reader.reader-types :refer
    [IndexingReader IPushbackReader Reader get-column-number
     get-file-name get-line-number read-char peek-char unread]]))

(deftype NodePushbackReader [readable]
  Reader
  (read-char [_]
    (str (.read readable 1)))
  (peek-char [this]
    (let [char (read-char this)]
      (unread this char)
      char))
  IPushbackReader
  (unread [_ char]
    (.unshift readable char)))
