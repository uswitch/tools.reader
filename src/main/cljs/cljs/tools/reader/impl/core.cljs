(ns cljs.tools.reader.impl.core)

(def last-id (atom 0))
(defn rt-next-id
  ^:stub
  []
  (swap! last-id inc))

(defprotocol IMutableList
  (concat! [this x])
  (prepend! [this x]))

(deftype MutableList [state]
  IMutableList
  (concat! [_ x]
    (swap! state concat x))
  (prepend! [_ x]
    (swap! state (partial concat x)))
  ISeq
  (-first [_]
    (first @state))
  (-rest [_]
    (rest @state))
  ISeqable
  (-seq [_] @state))

(defn mutable-list [& args]
  (MutableList. (atom args)))
