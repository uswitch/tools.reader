(ns cljs.tools.reader)

(declare syntax-quote*)

(defmacro syntax-quote
  "Macro equivalent to the syntax-quote reader macro (`)."
  [form]
  '(cljs.core/binding [cljs.tools.reader/gensym-env {}]
    (cljs.tools.reader/syntax-quote* ~form)))
