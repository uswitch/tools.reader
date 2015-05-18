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
              [goog.string :as gstring]))

(defn char [x]
  (when x
    (cljs.core/char x)))

;; getColumnNumber and *default-data-reader-fn* are available only since clojure-1.5.0-beta1

(defn cljs-version []
  (let [[major minor] (string/split *clojurescript-version* #"\.")
        [minor build] (string/split minor #"-")
        [major minor build] (map #(js/parseInt %) [major minor build])]
    {:major major
     :minor minor
     :build build}))

(def pre-cljs-1243 (-> (cljs-version) :build (< 1243)))

(defn ex-info? [ex]
  (instance? cljs.core.ExceptionInfo ex))

(defn starts-with? [s prefix]
  (gstring/startsWith s prefix))

#_(compile-if true ;pre-cljs-1243
;;; tagged-literal type arrived in CLJS-1243

  (do
    (defrecord TaggedLiteral [tag form])

    (defn tagged-literal?
      "Return true if the value is the data representation of a tagged literal"
      [value]
      (instance? clojure.tools.reader.impl.utils.TaggedLiteral value))

    (defn tagged-literal
      "Construct a data representation of a tagged literal from a
       tag symbol and a form."
      [tag form]
      (clojure.tools.reader.impl.utils.TaggedLiteral. tag form))

    ;; (ns-unmap *ns* '->TaggedLiteral)
    ;; (ns-unmap *ns* 'map->TaggedLiteral)

    (defmethod print-method clojure.tools.reader.impl.utils.TaggedLiteral [o w]
      (.write w "#")
      (print-method (:tag o) w)
      (.write w " ")
      (print-method (:form o) w))

    (defrecord ReaderConditional [splicing? form])
    ;; (ns-unmap *ns* '->ReaderConditional)
    ;; (ns-unmap *ns* 'map->ReaderConditional)

    (defn reader-conditional?
      "Return true if the value is the data representation of a reader conditional"
      [value]
      (instance? clojure.tools.reader.impl.utils.ReaderConditional value))

    (defn reader-conditional
      "Construct a data representation of a reader conditional.
       If true, splicing? indicates read-cond-splicing."
      [form splicing?]
      (clojure.tools.reader.impl.utils.ReaderConditional. splicing? form))

    (defmethod print-method clojure.tools.reader.impl.utils.ReaderConditional [o w]
      (.write w "#?")
      (when (:splicing? o) (.write w "@"))
      (print-method (:form o) w))))

(defn whitespace?
  "Checks whether a given character is whitespace"
  [ch]
  (when ch
    (or (gstring/isSpace ch)
        (identical? \,  ch))))

(defn numeric?
  "Checks whether a given character is numeric"
  [ch]
  (when ch
    (gstring/isNumeric ch)))

(defn newline?
  "Checks whether the character is a newline"
  [c]
  (or (identical? \newline c)
      (nil? c)))

(defn desugar-meta
  "Resolves syntactical sugar in metadata" ;; could be combined with some other desugar?
  [f]
  (cond
    (keyword? f) {f true}
    (symbol? f)  {:tag f}
    (string? f)  {:tag f}
    :else        f))
