(ns cljs.tools.reader)

(defn- syntax-quote* [form]
  (->>
   (cond
     (special-symbol? form) (list 'quote form)

     (symbol? form)
     (list 'quote
           (if (namespace form)
             (let [maybe-class ((ns-map *ns*)
                                (symbol (namespace form)))]
               (if (class? maybe-class)
                 (symbol (.getName maybe-class) (name form))
                 (resolve-symbol form)))
             (let [sym (name form)]
               (cond
                 (.endsWith sym "#")
                 (register-gensym form)

                 (.startsWith sym ".")
                 form

                 (.endsWith sym ".")
                 (let [csym (symbol (subs sym 0 (dec (count sym))))]
                   (symbol (.concat (name (resolve-symbol csym)) ".")))
                 :else (resolve-symbol form)))))

     (unquote? form) (second form)
     (unquote-splicing? form) (throw (IllegalStateException. "splice not in list"))

     (coll? form)
     (cond

       (instance? IRecord form) form
       (map? form) (syntax-quote-coll (map-func form) (flatten-map form))
       (vector? form) (list 'clojure.core/vec (syntax-quote-coll nil form))
       (set? form) (syntax-quote-coll 'clojure.core/hash-set form)
       (or (seq? form) (list? form))
       (let [seq (seq form)]
         (if seq
           (syntax-quote-coll nil seq)
           '(clojure.core/list)))

       :else (throw (UnsupportedOperationException. "Unknown Collection type")))

     (or (keyword? form)
         (number? form)
         ;; (char? form) ;; no char type in cljs
         (string? form)
         (nil? form)
         (bool? form)
         (instance? js/RegExp form))
     form

     :else (list 'quote form))
   (add-meta form)))

(defmacro syntax-quote
  "Macro equivalent to the syntax-quote reader macro (`)."
  [form]
  '(cljs.core/binding [cljs.tools.reader/gensym-env {}]
    (cljs.tools.reader/syntax-quote* ~form)))

(defn ^:private ns-name* [x]
  (if (instance? clojure.lang.Namespace x)
    (name (ns-name x))
    (name x)))

(defn ^:dynamic resolve-symbol
  "Resolve a symbol s into its fully qualified namespace version"
  [s]
  (if (pos? (.indexOf (name s) "."))
    s ;; If there is a period, it is interop
    (if-let [ns-str (namespace s)]
      (let [ns (resolve-ns (symbol ns-str))]
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
