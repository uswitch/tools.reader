(ns cljs.tools.reader-test
  (:refer-clojure :exclude [read-string])
  (:require
    [cljs.test :as t :refer-macros [deftest is run-tests]]
    [cljs.tools.reader :as reader :refer [*data-readers* read-string]]))

;;==============================================================================
;; common_tests.clj
;;==============================================================================

(deftest read-integer
  (is (== 42 (read-string "42")))
  (is (== +42 (read-string "+42")))
  (is (== -42 (read-string "-42")))

  (is (== 0 (read-string "0")))

  (is (== 042 (read-string "042")))
  (is (== +042 (read-string "+042")))
  (is (== -042 (read-string "-042")))

  ;;hex
  (is (== 0x42e (read-string "0x42e")))
  (is (== +0x42e (read-string "+0x42e")))
  (is (== -0x42e (read-string "-0x42e")))

  ;;oct
  (is (== 511 (js/parseInt "777" 8) (read-string "0777")))
  (is (== -511 (js/parseInt "-777" 8) (read-string "-0777")))
  (is (== 1340 (js/parseInt "02474" 8) (read-string "02474")))
  (is (== -1340 (js/parseInt "-02474" 8) (read-string "-02474")))

  ;;parse oct as decimal
  (is (== 888 (js/parseInt "0888" 10) (read-string "0888")))
  (is (== -888 (js/parseInt "-0888" 10) (read-string "-0888")))
  (is (== 4984 (js/parseInt "04984" 10) (read-string "04984")))
  (is (== -4984 (js/parseInt "-04984" 10) (read-string "-04984")))

  (comment
    ;;TODO: Do we want to enable binary numbers? It's an easy addition, and
    ;; they are available in chrome already

    ;;binary
    (is (== 2147483648
            (js/parseInt "10000000000000000000000000000000" 2)
            (read-string "0b10000000000000000000000000000000")))
    (is (== -2147483648
            (js/parseInt "-10000000000000000000000000000000" 2)
            (read-string "-0b10000000000000000000000000000000")))
    (is (== 2139095040
            (js/parseInt "01111111100000000000000000000000" 2)
            (read-string "0b01111111100000000000000000000000")))
    (is (== -2139095040
            (js/parseInt "-01111111100000000000000000000000" 2)
            (read-string "-0b01111111100000000000000000000000")))
    (is (== 8388607
            (js/parseInt "00000000011111111111111111111111" 2)
            (read-string "0B00000000011111111111111111111111")))
    (is (== -8388607
            (js/parseInt "-00000000011111111111111111111111" 2)
            (read-string "-0B00000000011111111111111111111111")))
    )
)

(deftest read-floating
  (is (== 42.23 (read-string "42.23")))
  (is (== +42.23 (read-string "+42.23")))
  (is (== -42.23 (read-string "-42.23")))

  (is (== 42.2e3 (read-string "42.2e3")))
  (is (== +42.2e+3 (read-string "+42.2e+3")))
  (is (== -42.2e-3 (read-string "-42.2e-3")))
)

(deftest read-ratio
  (is (== 4/2 (read-string "4/2")))
  (is (== 4/2 (read-string "+4/2")))
  (is (== -4/2 (read-string "-4/2")))
)

(deftest read-symbol
  (is (= 'foo (read-string "foo")))
  (is (= 'foo/bar (read-string "foo/bar")))
  (is (= '*+!-_? (read-string "*+!-_?")))
  (is (= 'abc:def:ghi (read-string "abc:def:ghi")))
  (is (= 'abc.def/ghi (read-string "abc.def/ghi")))
  (is (= 'abc/def.ghi (read-string "abc/def.ghi")))
  (is (= 'abc:def/ghi:jkl.mno (read-string "abc:def/ghi:jkl.mno")))
  (is (instance? cljs.core/Symbol (read-string "alphabet")))
  (is (= "foo//" (str (read-string "foo//"))))
  (is (js/isNaN (read-string "NaN"))) ;; not sure if this should be js/NaN
  (is (= js/Number.POSITIVE_INFINITY (read-string "Infinity"))) ;; not sure if this should be js version of Infinity
  (is (= js/Number.POSITIVE_INFINITY (read-string "+Infinity"))) ;; not sure if this should be js version of Infinity
  (is (= js/Number.NEGATIVE_INFINITY (read-string "-Infinity"))) ;; not sure if this should be js version of Infinity
)

(deftest read-specials
  (is (= 'nil nil))
  (is (= 'false false))
  (is (= 'true true))
)

(deftest read-char
  (is (= \f (read-string "\\f")))
  (is (= \u0194 (read-string "\\u0194")))
  (is (= \o123 (read-string "\\o123")))
  (is (= \newline (read-string "\\newline")))
  (is (= (char 0) (read-string "\\o0")))
  (is (= (char 0) (read-string "\\o000")))
  (is (= (char 0377) (read-string "\\o377")))
  (is (= \A (read-string "\\u0041")))
  (is (= \@ (read-string "\\@")))
  (is (= (char 0xd7ff) (read-string "\\ud7ff")))
  (is (= (char 0xe000) (read-string "\\ue000")))
  (is (= (char 0xffff) (read-string "\\uffff")))
)

(deftest read-string*
  (is (= "foo bar" (read-string "\"foo bar\"")))
  (is (= "foo\\bar" (read-string "\"foo\\\\bar\"")))
  (is (= "foo\000bar" (read-string "\"foo\\000bar\"")))
  (is (= "foo\u0194bar" (read-string "\"foo\\u0194bar\"")))
  (is (= "foo\123bar" (read-string "\"foo\\123bar\"")))
)

(deftest read-list
  (is (= '() (read-string "()")))
  (is (= '(foo bar) (read-string "(foo bar)")))
  (is (= '(foo (bar) baz) (read-string "(foo (bar) baz)")))
)

(deftest read-vector
  (is (= '[] (read-string "[]")))
  (is (= '[foo bar] (read-string "[foo bar]")))
  (is (= '[foo [bar] baz] (read-string "[foo [bar] baz]")))
)

(deftest read-map
  (is (= '{} (read-string "{}")))
  (is (= '{foo bar} (read-string "{foo bar}")))
  (is (= '{foo {bar baz}} (read-string "{foo {bar baz}}")))
)

(deftest read-set
  (is (= '#{} (read-string "#{}")))
  (is (= '#{foo bar} (read-string "#{foo bar}")))
  (is (= '#{foo #{bar} baz} (read-string "#{foo #{bar} baz}")))
)

(def *ns* 'user)

(deftest read-keyword
  (is (= :foo-bar (read-string ":foo-bar")))
  (is (= :foo/bar (read-string ":foo/bar")))
  ;; (is (= '::foo-bar (namespace (read-string "::foo-bar"))))
  ;; (is (= ^:resolve-ns :core/foo-bar (read-string "::core/foo-bar")))
  (is (= :*+!-_? (read-string ":*+!-_?")))
  (is (= :abc:def:ghi (read-string ":abc:def:ghi")))
  (is (= :abc.def/ghi (read-string ":abc.def/ghi")))
  (is (= :abc/def.ghi (read-string ":abc/def.ghi")))
  (is (= :abc:def/ghi:jkl.mno (read-string ":abc:def/ghi:jkl.mno")))
  (is (instance? cljs.core.Keyword (read-string ":alphabet"))) )

(deftest read-regex
  (is (= (str #"\[\]?(\")\\")
         (str (read-string "#\"\\[\\]?(\\\")\\\\\"")))))

(deftest read-quote
  (is (= ''foo (read-string "'foo"))))

#_(deftest read-syntax-quote ;;; can't do syntax quote yet - need namespace resolution
  (is (= '`user/foo (read-string "`foo"))) ;;  (binding [*ns* (the-ns 'user)] (read-string "`foo"))
  ;; (is (= () (eval (read-string "`(~@[])")))) ;;; no-eval
  (is (= '`+ (read-string "`+")))
  (is (= '`foo/bar (read-string "`foo/bar")))
  (is (= '`1 (read-string "`1")))
  ;;;(is (= `(1 (~2 ~@'(3))) (eval (read-string "`(1 (~2 ~@'(3)))")))) ;;; no eval
  )

(deftest read-deref
  (is (= '@foo (read-string "@foo"))))

(deftest read-var
  (is (= '(var foo) (read-string "#'foo"))))

#_(deftest read-fn ;; need thread-bound? (or the functionality required) to do this
  (is (= '(fn* [] (foo bar baz)) (read-string "#(foo bar baz)"))))

(defn inst [s]
  (js/Date. s))

(defn uuid [s]
  (cljs.core.UUID. s nil))

(deftest read-tagged
  (binding [*data-readers* {'inst inst 'uuid uuid}]
    (is (= #inst "2010-11-12T13:14:15.666"
           (read-string "#inst \"2010-11-12T13:14:15.666\"")))
    (is (= #inst "2010-11-12T13:14:15.666"
           (read-string "#inst\"2010-11-12T13:14:15.666\"")))
    ;; (is (= #uuid "550e8400-e29b-41d4-a716-446655440000"
    ;;        (read-string "#uuid \"550e8400-e29b-41d4-a716-446655440000\"")))
    ;; (is (= #uuid "550e8400-e29b-41d4-a716-446655440000"
    ;;        (read-string "#uuid\"550e8400-e29b-41d4-a716-446655440000\"")))
    (is (= (uuid "550e8400-e29b-41d4-a716-446655440000")
           (read-string "#uuid \"550e8400-e29b-41d4-a716-446655440000\"")))
    (is (= (uuid "550e8400-e29b-41d4-a716-446655440000")
           (read-string "#uuid\"550e8400-e29b-41d4-a716-446655440000\"")))
    #_(when *default-data-reader-fn*
      (let [my-unknown (fn [tag val] {:unknown-tag tag :value val})]
        (is (= {:unknown-tag 'foo :value 'bar}
               (binding [*default-data-reader-fn* my-unknown]
                 (read-string "#foo bar"))))))))

(defrecord foo [])
(defrecord bar [baz buz])

#_(deftest read-record
  (prn 'clojure.tools.reader_test.foo.)
  (is (= (foo.) (read-string "#clojure.tools.reader_test.foo[]")))
  (is (= (foo.) (read-string "#clojure.tools.reader_test.foo []"))) ;; not valid in clojure
  (is (= (foo.) (read-string "#clojure.tools.reader_test.foo{}")))
  (is (= (assoc (foo.) :foo 'bar) (read-string "#clojure.tools.reader_test.foo{:foo bar}")))

  (is (= (map->bar {}) (read-string "#clojure.tools.reader_test.bar{}")))
  (is (= (bar. 1 nil) (read-string "#clojure.tools.reader_test.bar{:baz 1}")))
  (is (= (bar. 1 nil) (read-string "#clojure.tools.reader_test.bar[1 nil]")))
  (is (= (bar. 1 2) (read-string "#clojure.tools.reader_test.bar[1 2]"))))

(prn (reader/read (cljs.tools.reader.reader-types/source-logging-push-back-reader "(def test 8)\n(def test2 9)")))

(enable-console-print!)
(run-tests)
