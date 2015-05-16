(ns cljs.tools.reader.impl.utils)

(defmacro compile-if [cond then & [else]]
  (if (eval cond)
    then
    else))
