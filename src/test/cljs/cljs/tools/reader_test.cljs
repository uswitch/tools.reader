(ns cljs.tools.reader-test
  (:refer-clojure :exclude [read-string])
  (:require
    [cljs.test :as t :refer-macros [are deftest is run-tests testing]]
    [cljs.tools.reader :as reader :refer
     [*data-readers* read-string ->UnresolvedKeyword
      ->UnresolvedSymbol ->SyntaxQuotedForm ->ReadRecord
      reader-conditional reader-conditional?]]
    [cljs.tools.reader.reader-types :as rt]))

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

(deftest read-keyword
  (is (= :foo-bar (read-string ":foo-bar")))
  (is (= :foo/bar (read-string ":foo/bar")))
  (is (= (->UnresolvedKeyword nil "foo-bar") (read-string "::foo-bar")))
  (is (= (->UnresolvedKeyword "core" "foo-bar") (read-string "::core/foo-bar")))
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

(deftest read-syntax-quote
  (let [q (read-string "quote")]
    (is (= q (first (read-string "`foo"))))
    (is (= (->UnresolvedSymbol nil 'foo) (second (read-string "`foo"))))

    ;; (is (= () (eval (read-string "`(~@[])")))) ;;; no-eval

    (is (= q (first (read-string "`+"))))
    (is (= (->UnresolvedSymbol nil '+) (second (read-string "`+"))))

    (is (= q (first (read-string "`foo/bar"))))
    (is (= (->UnresolvedSymbol 'foo 'bar) (second (read-string "`foo/bar"))))

    (is (= 1 (read-string "`1"))))
  ;;;(is (= `(1 (~2 ~@'(3))) (eval (read-string "`(1 (~2 ~@'(3)))")))) ;;; no eval
  )

(deftest read-deref
  (is (= '@foo (read-string "@foo"))))

(deftest read-var
  (is (= '(var foo) (read-string "#'foo"))))

(deftest read-fn ;; need thread-bound? (or the functionality required) to do this
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

(deftest read-record
  (is (= (->ReadRecord "cljs.tools.reader_test" "foo" :short [])
         (read-string "#cljs.tools.reader_test.foo[]")))
  (is (= (->ReadRecord "cljs.tools.reader_test" "foo" :short [])
         (read-string "#cljs.tools.reader_test.foo []"))) ;; not valid in clojure
  (is (= (->ReadRecord "cljs.tools.reader_test" "foo" :extended {})
         (read-string "#cljs.tools.reader_test.foo{}")))
  (is (= (->ReadRecord "cljs.tools.reader_test" "foo" :extended {:foo 'bar})
         (read-string "#cljs.tools.reader_test.foo{:foo bar}")))

  (is (= (map->bar {})
         (meta (reader/read (rt/source-logging-push-back-reader "#clojure.tools.reader_test.bar{}") false nil))))
  (is (= (->ReadRecord "cljs.tools.reader_test" "foo" :extended {:baz 1})
         (read-string "#clojure.tools.reader_test.bar{:baz 1}")))
  (is (= (bar. 1 nil) (read-string "#clojure.tools.reader_test.bar[1 nil]")))
  (is (= (bar. 1 2) (read-string "#clojure.tools.reader_test.bar[1 2]"))))

(deftest source-logging-meta-test
  (-> (loop [r (cljs.tools.reader.reader-types/source-logging-push-back-reader "(def test 8)\n(def test2 9)\n")
             forms []]
        (if-let [form (reader/read r false nil)]
          (recur r (conj forms [(meta form) form]))
          forms))
      (= [[{:line 1 :column 1 :end-line 1 :end-column 13} '(def test 8)]
          [{:line 2 :column 0 :end-line 2 :end-column 1}]
          [{:line 2, :column 1, :end-line 2, :end-column 14} '(def test2 9)]
          [{:line 3, :column 0, :end-line 3, :end-column 1}]])))

(defrecord JSValue [v])

(deftest reader-conditionals
  (let [opts {:read-cond :allow :features #{:clj}}]
    (are [out s opts] (= out (read-string opts s))
         ;; basic read-cond
         '[foo-form] "[#?(:foo foo-form :bar bar-form)]" {:read-cond :allow :features #{:foo}}
         '[bar-form] "[#?(:foo foo-form :bar bar-form)]" {:read-cond :allow :features #{:bar}}
         '[foo-form] "[#?(:foo foo-form :bar bar-form)]" {:read-cond :allow :features #{:foo :bar}}
         '[] "[#?(:foo foo-form :bar bar-form)]" {:read-cond :allow :features #{:baz}}

         ;; environmental features
         "clojure" "#?(:clj \"clojure\" :cljs \"clojurescript\" :default \"default\")"  opts

         ;; default features
         "default" "#?(:cljr \"clr\" :cljs \"cljs\" :default \"default\")" opts

         ;; splicing
         [] "[#?@(:clj [])]" opts
         [:a] "[#?@(:clj [:a])]" opts
         [:a :b] "[#?@(:clj [:a :b])]" opts
         [:a :b :c] "[#?@(:clj [:a :b :c])]" opts

         ;; nested splicing
         [:a :b :c :d :e] "[#?@(:clj [:a #?@(:clj [:b #?@(:clj [:c]) :d]):e])]" opts
         '(+ 1 (+ 2 3)) "(+ #?@(:clj [1 (+ #?@(:clj [2 3]))]))" opts
         '(+ (+ 2 3) 1) "(+ #?@(:clj [(+ #?@(:clj [2 3])) 1]))" opts
         [:a [:b [:c] :d] :e] "[#?@(:clj [:a [#?@(:clj [:b #?@(:clj [[:c]]) :d])] :e])]" opts

         ;; bypass unknown tagged literals
         [1 2 3] "#?(:cljs #js [1 2 3] :clj [1 2 3])" opts
         :clojure "#?(:foo #some.nonexistent.Record {:x 1} :clj :clojure)" opts)

    (are [re s opts] (is (thrown-with-msg? js/Error re (read-string opts s)))
         #"Feature should be a keyword" "#?((+ 1 2) :a)" opts
         #"even number of forms" "#?(:cljs :a :clj)" opts
         #"read-cond-splicing must implement" "#?@(:clj :a)" opts
         #"is reserved" "#?@(:foo :a :else :b)" opts
         #"must be a list" "#?[:foo :a :else :b]" opts
         #"Conditional read not allowed" "#?[:clj :a :default nil]" {:read-cond :BOGUS}
         #"Conditional read not allowed" "#?[:clj :a :default nil]" {})

    #_(are [re type s opts] (is (ex-match? (try
                                           (read-string opts s)
                                           (catch cljs.core.ExceptionInfo e e))
                                         type
                                         re))
         #"Feature should be a keyword" :reader-exception "#?((+ 1 2) :a)" opts
         #"Feature should be a keyword" :reader-exception"#?((+ 1 2) :a)" opts
         #"even number of forms" :reader-exception "#?(:cljs :a :clj)" opts
         #"read-cond-splicing must implement" :reader-exception "#?@(:clj :a)" opts
         #"is reserved" :reader-exception "#?@(:foo :a :else :b)" opts
         #"must be a list" :runtime-exception "#?[:foo :a :else :b]" opts
         #"Conditional read not allowed" :runtime-exception "#?[:clj :a :default nil]" {:read-cond :BOGUS}
         #"Conditional read not allowed" :runtime-exception "#?[:clj :a :default nil]" {}))
  (binding [*data-readers* {'js (fn [v] (JSValue. v) )}]
    (is (= (JSValue. [1 2 3])
           (read-string {:features #{:cljs} :read-cond :allow} "#?(:cljs #js [1 2 3] :foo #foo [1])")))))

(deftest preserve-read-cond
  (is (= 1 (binding [*data-readers* {'foo (constantly 1)}]
             (read-string {:read-cond :preserve} "#foo []"))))

  (let [x (read-string {:read-cond :preserve} "#?(:clj foo :cljs bar)")]
    (is (reader-conditional? x))
    (is (= x (reader-conditional '(:clj foo :cljs bar) false)))
    (is (not (:splicing? x)))
    (is (= :foo (get x :no-such-key :foo)))
    (is (= (:form x) '(:clj foo :cljs bar))))
  (let [x (read-string {:read-cond :preserve} "#?@(:clj [foo])" )]
    (is (reader-conditional? x))
    (is (= x (reader-conditional '(:clj [foo]) true)))
    (is (:splicing? x))
    (is (= :foo (get x :no-such-key :foo)))
    (is (= (:form x) '(:clj [foo]))))
  (is (thrown-with-msg? js/Error #"No reader function for tag"
                        (read-string {:read-cond :preserve} "#js {:x 1 :y 2}" )))
  (let [x (read-string {:read-cond :preserve} "#?(:cljs #js {:x 1 :y 2})")
        [platform tl] (:form x)]
    (is (reader-conditional? x))
    (is (tagged-literal? tl))
    (is (= tl (tagged-literal 'js {:x 1 :y 2})))
    (is (= 'js (:tag tl)))
    (is (= {:x 1 :y 2} (:form tl)))
    (is (= :foo (get tl :no-such-key :foo))))
  (testing "print form roundtrips"
    (doseq [s ["#?(:clj foo :cljs bar)"
               "#?(:cljs #js {:x 1, :y 2})"
               "#?(:clj #clojure.test_clojure.reader.TestRecord [42 85])"]]
      (is (= s (pr-str (read-string {:read-cond :preserve} s)))))))

(enable-console-print!)
(run-tests)
