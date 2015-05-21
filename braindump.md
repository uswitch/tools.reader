# Status of ClojureScript tools.reader 21st of May, 2015

## Additions

- indexing/sourcelogging "working"
- metadata reading
- most tests passing
- extended number parsing to support exponents: 0.314e+1 - reduced to
  js/Number

## Not working

- Records can't be instantiated without eval, should use a wrapping
  datastructure such as:
  `(defrecord RecordConstructor [namespace name values])`

## Questions - answered

- Should we implement `inst` and `uuid` or is that a ClojureScript
  compiler concern?
  => I think this is out of scope. `inst` and `uuid` are cljs.core fns,
     the *data-readers* mapping can be passed by the user, or the
     compiler.
- How is ClojureScript going to pass in namespace resolution? How does
  tools.reader augment the namespace resolution? Who's responsibility is
  maintaining environment e.g. for `syntax-quote`?
  => swannodette: @bostonou @andrewmcveigh @tgkristensen as @bronsa_
     alluded, discard anything incompatible w/ CLJS compilation/runtime
     model
     https://twitter.com/swannodette/status/600358954196144128
  => passing wrapping datastructures to the compiler seems reasonable
     swannodette: @bostonou @andrewmcveigh @bronsa_ @tgkristensen it's
     useful in that you can pass read forms on to the compiler :)
     https://twitter.com/swannodette/status/600356869467742210
- Are we going to get an `eval` in ClojureScript? tools.reader
  implements `#=(...)` which requires `eval`, so we don't know if we
  should drop that support?
  => as above

# Status of ClojureScript tools.reader 18th of May, 2015

Progress has been made on porting tools.reader to ClojureScript. The
original project has been cloned and we have removed Java-specific
code to the point where the resulting library compiles and is able to do
the following:

- Parse EDN without `#inst` and `#uuid` literals - we are not certain
  that those readers should live in tools.reader or in the compiler.
- Integers, double, floats are parsed, everything is reduced to a
  JavaScript number, so no BigInt etc. support. Ratios are also parsed,
  but they are also reduced to JavaScript numbers.
- Symbols are parsed but we cannot resolve namespaces as we don't have a
  namespace to resolve it in. (Should CS compiling in CS provide the
  namespace to us on some form?)
- Keywords are also parsed, but again, can be namespaced so the same
  issue applies.
- Special symbols (nil, false, true) all work.
- Characters work, but as JavaScript doesn't have a char-type so they
  are parsed to a one-char string.
- Strings work.
- Data-structures (list, maps, vectors, sets) work.
- RegExs work and pass the original test.
- Quote works.
- Deref and var work. `"#'foo"` reads as `'(var foo)` and `"@foo"` reads
  as `@foo`.

## Should do before asking questions

- Records might work, but we have not tested it.
- Tagged readers should work but not tested.

## Open questions

- Should we implement `inst` and `uuid` or is that a ClojureScript
  compiler concern?
- How is ClojureScript going to pass in namespace resolution? How does
  tools.reader augment the namespace resolution? Who's responsibility is
  maintaining environment e.g. for `syntax-quote`?
- Are we going to get an `eval` in ClojureScript? tools.reader
  implements `#=(...)` which requires `eval`, so we don't know if we
  should drop that support?
