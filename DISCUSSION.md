# Issues

## Types

Types such as Char, BigInt, BigDec don't exist in cljs, what to do?

## Namespaces

Not much namespace functionality exists in cljs, most (or all) is at
the clojure/macro level only. This pretty much rules out ns resolution
of symbols, and keywords. Also Clojure's syntax-quote is dependent on
ns-resolving symbols.

From memory, Namespace/Symbol resolution is not really hard, but would
mean re-implementing core parts of cljs.
