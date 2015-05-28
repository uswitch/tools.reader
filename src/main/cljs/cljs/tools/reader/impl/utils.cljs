;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns cljs.tools.reader.impl.utils
    (:refer-clojure :exclude [char])
    (:require [clojure.string :as string]
              [goog.string]))

(defn char [x]
  (when x
    (cljs.core/char x)))

(defn ex-info? [ex]
  (instance? cljs.core.ExceptionInfo ex))

(defrecord ReaderConditional [splicing? form])

(defn reader-conditional?
  "Return true if the value is the data representation of a reader conditional"
  [value]
  (instance? ReaderConditional value))

(defn reader-conditional
  "Construct a data representation of a reader conditional.
  If true, splicing? indicates read-cond-splicing."
  [form splicing?]
  (ReaderConditional. splicing? form))

(extend-protocol IPrintWithWriter
  ReaderConditional
  (-pr-writer [coll writer opts]
    (pr-sequential-writer writer
                          pr-writer ; this is private in cljs.core?
                          (str "#?" (when (:splicing? coll) "@") "(")
                          " "
                          ")"
                          opts
                          (:form coll))))

(defn whitespace?
  "Checks whether a given character is whitespace"
  [ch]
  (when ch
    (let [white-chars
          [\,
           \space
           "\t"         ;;, U+0009 HORIZONTAL TABULATION.
           "\n"         ;;, U+000A LINE FEED.
           "\u000B"     ;;, U+000B VERTICAL TABULATION.
           "\f"         ;;, U+000C FORM FEED.
           "\r"         ;;, U+000D CARRIAGE RETURN.
           "\u001C"     ;;, U+001C FILE SEPARATOR.
           "\u001D"     ;;, U+001D GROUP SEPARATOR.
           "\u001E"     ;;, U+001E RECORD SEPARATOR.
           "\u001F"]]  ;;, U+001F UNIT SEPARATOR
      (some (partial identical? ch) white-chars))))

(defn numeric?
  "Checks whether a given character is numeric"
  [ch]
  (when ch
    (goog.string/isNumeric ch)))

(defn newline?
  "Checks whether the character is a newline"
  [c]
  (or (identical? \newline c)
      (identical? "\n" c)
      (nil? c)))

(defn desugar-meta
  "Resolves syntactical sugar in metadata" ;; could be combined with some other desugar?
  [f]
  (cond
    (keyword? f) {f true}
    (symbol? f)  {:tag f}
    (string? f)  {:tag f}
    :else        f))

(def last-id (atom 0))

(defn next-id
  []
  (swap! last-id inc))
