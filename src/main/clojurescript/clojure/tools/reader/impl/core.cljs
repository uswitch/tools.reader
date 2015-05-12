(ns clojure.tools.reader.impl.core)

(def Exception js/Error)

(defn ^{:jsdoc ["@constructor"]}
  IllegalArgumentException
  ([message] (IllegalArgumentException message nil nil))
  ([message data cause]
   (let [e (js/Error.)]
     (this-as this
              (set! (.-message this) message)
              (set! (.-data this) data)
              (set! (.-cause this) cause)
              (do
                (set! (.-name this) (.-name e))
                ;; non-standard
                (set! (.-description this) (.-description e))
                (set! (.-number this) (.-number e))
                (set! (.-fileName this) (.-fileName e))
                (set! (.-lineNumber this) (.-lineNumber e))
                (set! (.-columnNumber this) (.-columnNumber e))
                (set! (.-stack this) (.-stack e)))
              this))))

(set! (.. IllegalArgumentException -prototype -__proto__) js/Error.prototype)

(set! (.. IllegalArgumentException -prototype -toString)
  (fn []
    (this-as this (pr-str* this))))

(defn ^{:jsdoc ["@constructor"]}
  RuntimeException
  ([message]
   (let [e (js/Error.)]
     (this-as this
              (set! (.-message this) message)
              (do
                (set! (.-name this) (.-name e))
                ;; non-standard
                (set! (.-description this) (.-description e))
                (set! (.-number this) (.-number e))
                (set! (.-fileName this) (.-fileName e))
                (set! (.-lineNumber this) (.-lineNumber e))
                (set! (.-columnNumber this) (.-columnNumber e))
                (set! (.-stack this) (.-stack e)))
              this))))

(set! (.. RuntimeException -prototype -__proto__) js/Error.prototype)

(set! (.. RuntimeException -prototype -toString)
  (fn []
    (this-as this (pr-str* this))))

(defn ^{:jsdoc ["@constructor"]}
  IllegalStateException
  ([message]
   (let [e (js/Error.)]
     (this-as this
              (set! (.-message this) message)
              (do
                (set! (.-name this) (.-name e))
                ;; non-standard
                (set! (.-description this) (.-description e))
                (set! (.-number this) (.-number e))
                (set! (.-fileName this) (.-fileName e))
                (set! (.-lineNumber this) (.-lineNumber e))
                (set! (.-columnNumber this) (.-columnNumber e))
                (set! (.-stack this) (.-stack e)))
              this))))

(set! (.. IllegalStateException -prototype -__proto__) js/Error.prototype)

(set! (.. IllegalStateException -prototype -toString)
  (fn []
    (this-as this (pr-str* this))))

(defn persistent-hash-set-create-with-check
  ^:stub
  [coll]
  (set coll))

(defn persistent-list-create
  ^:stub
  [coll]
  (apply list coll))

(defn rt-map
  ^:stub
  [coll]
  (apply hash-map coll))

(defn integer-to-string
  ^:stub
  ([s] (str s))
  ([s base]
   (.toString s base)))

(defprotocol IStringBuilder
  (append [this s]))

(deftype StringBuilder [state]
  IStringBuilder
  (append [_ s]
    (str state s)))

(defn string-builder
  ([] (string-builder ""))
  ([s] (StringBuilder. s)))

(defn char-digit
  ^:stub
  [code-point radix])

(def last-id (atom 0))
(defn rt-next-id
  ^:stub
  []
  (swap! last-id inc))
