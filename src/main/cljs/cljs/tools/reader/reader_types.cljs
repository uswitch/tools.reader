;;   Copyright (c) Nicola Mometto, Rich Hickey & contributors.
;;   The use and distribution terms for this software are covered by the
;;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;;   which can be found in the file epl-v10.html at the root of this distribution.
;;   By using this software in any fashion, you are agreeing to be bound by
;;   the terms of this license.
;;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Protocols and default Reader types implementation"
      :author "Bronsa"}
  cljs.tools.reader.reader-types
  (:refer-clojure :exclude [char read-line])
  (:require-macros
   [cljs.tools.reader.impl.utils :refer [compile-if]]
   [cljs.tools.reader.reader-types :refer [update!]])
  (:require
   [cljs.tools.reader.impl.utils :refer
    [char whitespace? newline? make-var]]
   [cljs.tools.reader.impl.core :refer [RuntimeException]])
  (:import
   [goog.string StringBuffer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reader protocols
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Reader
  (read-char [reader]
    "Returns the next char from the Reader, nil if the end of stream has been reached")
  (peek-char [reader]
    "Returns the next char from the Reader without removing it from the reader stream"))

(defprotocol IPushbackReader
  (unread [reader ch]
    "Pushes back a single character on to the stream"))

(defprotocol IndexingReader
  (get-line-number [reader]
    "Returns the line number of the next character to be read from the stream")
  (get-column-number [reader]
    "Returns the column number of the next character to be read from the stream")
  (get-file-name [reader]
    "Returns the file name the reader is reading from, or nil"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; reader deftypes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftype StringReader
    [s s-len ^:unsynchronized-mutable s-pos]
  Reader
  (read-char [reader]
    (when (> s-len s-pos)
      (let [r (nth s s-pos)]
        (update! s-pos inc)
        r)))
  (peek-char [reader]
    (when (> s-len s-pos)
      (nth s s-pos))))

(deftype PushbackReader
    [rdr buf buf-len ^:unsynchronized-mutable buf-pos]
  Reader
  (read-char [reader]
    (char
     (if (< buf-pos buf-len)
       (let [r (aget buf buf-pos)]
         (update! buf-pos inc)
         r)
       (read-char rdr))))
  (peek-char [reader]
    (char
     (if (< buf-pos buf-len)
       (aget buf buf-pos)
       (peek-char rdr))))
  IPushbackReader
  (unread [reader ch]
    (when ch
      (if (zero? buf-pos) (throw (RuntimeException. "Pushback buffer is full")))
      (update! buf-pos dec)
      (aset buf buf-pos ch))))

(defn- normalize-newline [rdr ch]
  (if (identical? \return ch)
    (let [c (peek-char rdr)]
      (when (or (identical? \formfeed c)
                (identical? \newline c))
        (read-char rdr))
      \newline)
    ch))

(deftype IndexingPushbackReader
    [rdr ^:unsynchronized-mutable line ^:unsynchronized-mutable column
     ^:unsynchronized-mutable line-start? ^:unsynchronized-mutable prev
     ^:unsynchronized-mutable prev-column file-name]
  Reader
  (read-char [reader]
    (when-let [ch (read-char rdr)]
      (let [ch (normalize-newline rdr ch)]
        (set! prev line-start?)
        (set! line-start? (newline? ch))
        (when line-start?
          (set! prev-column column)
          (set! column 0)
          (update! line inc))
        (update! column inc)
        ch)))

  (peek-char [reader]
    (peek-char rdr))

  IPushbackReader
  (unread [reader ch]
    (if line-start?
      (do (update! line dec)
          (set! column prev-column))
      (update! column dec))
    (set! line-start? prev)
    (unread rdr ch))

  IndexingReader
  (get-line-number [reader] (int line))
  (get-column-number [reader] (int column))
  (get-file-name [reader] file-name))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Source Logging support
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn merge-meta
  "Returns an object of the same type and value as `obj`, with its
metadata merged over `m`."
  [obj m]
  (let [orig-meta (meta obj)]
    (with-meta obj (merge m (dissoc orig-meta :source)))))

(defn- peek-source-log
  "Returns a string containing the contents of the top most source
logging frame."
  [source-log-frames]
  (let [current-frame @source-log-frames]
    (subs (:buffer current-frame) (:offset current-frame))))

(defn- log-source-char
  "Logs `char` to all currently active source logging frames."
  [source-log-frames char]
  (when-let [buffer (:buffer @source-log-frames)]
    (.append buffer char)))

(defn- drop-last-logged-char
  "Removes the last logged character from all currently active source
logging frames. Called when pushing a character back."
  [source-log-frames]
  (when-let [buffer (:buffer @source-log-frames)]
    (.deleteCharAt buffer (dec (.getLength buffer)))))

(deftype SourceLoggingPushbackReader
    [rdr ^:unsynchronized-mutable line ^:unsynchronized-mutable column
     ^:unsynchronized-mutable line-start? ^:unsynchronized-mutable prev
     ^:unsynchronized-mutable prev-column file-name source-log-frames]
  Reader
  (read-char [reader]
    (when-let [ch (read-char rdr)]
      (let [ch (normalize-newline rdr ch)]
        (set! prev line-start?)
        (set! line-start? (newline? ch))
        (when line-start?
          (set! prev-column column)
          (set! column 0)
          (update! line inc))
        (update! column inc)
        (log-source-char source-log-frames ch)
        ch)))

  (peek-char [reader]
    (peek-char rdr))

  IPushbackReader
  (unread [reader ch]
    (if line-start?
      (do (update! line dec)
          (set! column prev-column))
      (update! column dec))
    (set! line-start? prev)
    (when ch
      (drop-last-logged-char source-log-frames))
    (unread rdr ch))

  IndexingReader
  (get-line-number [reader] (int line))
  (get-column-number [reader] (int column))
  (get-file-name [reader] file-name))

(defn log-source*
  [reader f]
  (let [frame (.-source-log-frames reader)
        _ (.log js/console frame)
        buffer (:buffer @frame)
        new-frame (assoc-in @frame [:offset] (.getLength buffer))]
    (let [frame new-frame]
      (let [ret (f)]
        (if (instance? clojure.lang.IMeta ret)
          (merge-meta ret {:source (peek-source-log frame)})
          ret)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; fast check for provided implementations
(defn indexing-reader?
  "Returns true if the reader satisfies IndexingReader"
  [rdr]
  (satisfies? IndexingReader rdr))

(defn string-reader
  "Creates a StringReader from a given string"
  ([s]
     (StringReader. s (count s) 0)))

(defn string-push-back-reader
  "Creates a PushbackReader from a given string"
  ([s]
     (string-push-back-reader s 1))
  ([s buf-len]
     (PushbackReader. (string-reader s) (object-array buf-len) buf-len buf-len)))

(defn indexing-push-back-reader
  "Creates an IndexingPushbackReader from a given string or PushbackReader"
  ([s-or-rdr]
     (indexing-push-back-reader s-or-rdr 1))
  ([s-or-rdr buf-len]
     (indexing-push-back-reader s-or-rdr buf-len nil))
  ([s-or-rdr buf-len file-name]
     (IndexingPushbackReader.
      (if (string? s-or-rdr) (string-push-back-reader s-or-rdr buf-len) s-or-rdr) 1 1 true nil 0 file-name)))

(defn source-logging-push-back-reader
  "Creates a SourceLoggingPushbackReader from a given string or PushbackReader"
  ([s-or-rdr]
     (source-logging-push-back-reader s-or-rdr 1))
  ([s-or-rdr buf-len]
     (source-logging-push-back-reader s-or-rdr buf-len nil))
  ([s-or-rdr buf-len file-name]
     (SourceLoggingPushbackReader.
      (if (string? s-or-rdr) (string-push-back-reader s-or-rdr buf-len) s-or-rdr)
      1
      1
      true
      nil
      0
      file-name
      (atom {:buffer (StringBuffer.) :offset 0}))))

(defn read-line
  "Reads a line from the reader or from *in* if no reader is specified"
  ([rdr]
   (loop [c (read-char rdr) s (StringBuffer.)]
     (if (newline? c)
       (str s)
       (recur (read-char rdr) (.append s c))))))

(defn reader-error
  "Throws an ExceptionInfo with the given message.
   If rdr is an IndexingReader, additional information about column and line number is provided"
  [rdr & msg]
  (throw (ex-info (apply str msg)
                  (merge {:type :reader-exception}
                         (when (indexing-reader? rdr)
                           (merge
                            {:line (get-line-number rdr)
                             :column (get-column-number rdr)}
                            (when-let [file-name (get-file-name rdr)]
                              {:file file-name})))))))

(defn source-logging-reader?
  [rdr]
  (instance? SourceLoggingPushbackReader rdr))

(defn line-start?
  "Returns true if rdr is an IndexingReader and the current char starts a new line"
  [rdr]
  (when (indexing-reader? rdr)
    (== 1 (get-column-number rdr))))