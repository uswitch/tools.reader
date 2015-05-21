(ns cljs.tools.reader.impl.core
  (:require [goog.array]))

(def last-id (atom 0))
(defn rt-next-id
  ^:stub
  []
  (swap! last-id inc))

(defprotocol IMutableList
  (prepend! [this x]))

(deftype MutableList [state]
  IMutableList
  (prepend! [_ x]
    (goog.array/extend x state))
  ISeqable
  (-seq [_] (seq state)))

(defn mutable-list [& args]
  (MutableList. (to-array args)))
