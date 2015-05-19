(ns cljs.tools.reader)

(defn ^:private ns-name* [x]
  (if (instance? clojure.lang.Namespace x)
    (name (ns-name x))
    (name x)))

#_(defn ^:dynamic resolve-symbol
  "Resolve a symbol s into its fully qualified namespace version"
  [s]
  (if (pos? (.indexOf (name s) "."))
    s ;; If there is a period, it is interop
    (if-let [ns-str (namespace s)]
      (let [ns (cljs.tools.reader/resolve-ns (symbol ns-str))]
        (if (or (nil? ns)
                (= (ns-name* ns) ns-str)) ;; not an alias
          s
          (symbol (ns-name* ns) (name s))))
      (if-let [o ((ns-map *ns*) s)]
        (if (class? o)
          (symbol (.getName o))
          (if (var? o)
            (symbol (-> o .ns ns-name*) (-> o .sym name))))
        (symbol (ns-name* *ns*) (name s))))))

(declare syntax-quote*)

(defmacro syntax-quote
  "Macro equivalent to the syntax-quote reader macro (`)."
  [form]
  '(cljs.core/binding [cljs.tools.reader/gensym-env {}]
    (cljs.tools.reader/syntax-quote* ~form)))
