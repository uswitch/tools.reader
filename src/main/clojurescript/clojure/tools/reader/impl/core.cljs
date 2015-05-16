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
  (append [this s]
    (swap! state str s)
    this)
  Object
  (toString [this] @state))

(defn string-builder
  ([] (string-builder ""))
  ([s] (StringBuilder. (atom s))))

(defn char-digit
  ^:stub
  [char radix]
  (js/parseInt char radix))

(defn char-value-of
  ^:stub
  [char]
  char)

(def last-id (atom 0))
(defn rt-next-id
  ^:stub
  []
  (swap! last-id inc))

(defn thread-bound?
  "Returns true if all of the vars provided as arguments have thread-local bindings.
   Implies that set!'ing the provided vars will succeed.  Returns true if no vars are provided."
  {:added "1.2"
   :static true}
  [& vars]
  (every? #(.getThreadBinding %) vars))

(defn starts-with? [s match]
  (re-find (re-pattern match) s))

(defprotocol IMutableList
  (concat! [this x])
  (prepend! [this x]))

(deftype MutableList [state]
  IMutableList
  (concat! [_ x]
    (swap! state concat x))
  (prepend! [_ x]
    (swap! state (partial concat x))))

(defn mutable-list [& args]
  (MutableList. args))
