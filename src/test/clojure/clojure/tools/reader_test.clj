(ns clojure.tools.reader-test
  (:refer-clojure :exclude [read read-string *default-data-reader-fn* *data-readers*])
  (:use [clojure.tools.reader :only [read read-string *default-data-reader-fn* *data-readers*]]
        [clojure.tools.reader.reader-types :only [string-push-back-reader]]
        [clojure.test :only [deftest is are testing]]
        [clojure.tools.reader.impl.utils :exclude [char]])
  (:import clojure.lang.BigInt))

(load "common_tests")

(deftest read-keyword
  (is (= :foo-bar (read-string ":foo-bar")))
  (is (= :foo/bar (read-string ":foo/bar")))
  (is (= :user/foo-bar (binding [*ns* (the-ns 'user)]
                         (read-string "::foo-bar"))))
  (is (= :clojure.core/foo-bar
         (do (alias 'core 'clojure.core)
             (read-string "::core/foo-bar"))))
  (is (= :*+!-_? (read-string ":*+!-_?")))
  (is (= :abc:def:ghi (read-string ":abc:def:ghi")))
  (is (= :abc.def/ghi (read-string ":abc.def/ghi")))
  (is (= :abc/def.ghi (read-string ":abc/def.ghi")))
  (is (= :abc:def/ghi:jkl.mno (read-string ":abc:def/ghi:jkl.mno")))
  (is (instance? clojure.lang.Keyword (read-string ":alphabet"))) )

(deftest read-regex
  (is (= (str #"\[\]?(\")\\")
         (str (read-string "#\"\\[\\]?(\\\")\\\\\"")))))

(deftest read-quote
  (is (= ''foo (read-string "'foo"))))

(deftest read-syntax-quote
  (is (= '`user/foo (binding [*ns* (the-ns 'user)]
                      (read-string "`foo"))))
  (is (= () (eval (read-string "`(~@[])"))))
  (is (= '`+ (read-string "`+")))
  (is (= '`foo/bar (read-string "`foo/bar")))
  (is (= '`1 (read-string "`1")))
  (is (= `(1 (~2 ~@'(3))) (eval (read-string "`(1 (~2 ~@'(3)))")))))

(deftest read-deref
  (is (= '@foo (read-string "@foo"))))

(deftest read-var
  (is (= '(var foo) (read-string "#'foo"))))

(deftest read-fn
  (is (= '(fn* [] (foo bar baz)) (read-string "#(foo bar baz)"))))

(deftest read-arg
  (is (= 14 ((eval (read-string "#(apply + % %1 %3 %&)")) 1 2 3 4 5)))
  (is (= 4 ((eval (read-string "#(last %&)")) 1 2 3 4))))

(deftest read-eval
  (is (= 3 (read-string "#=(+ 1 2)"))))

(deftest read-tagged
  ;; (is (= #inst "2010-11-12T13:14:15.666"
  ;;        (read-string "#inst \"2010-11-12T13:14:15.666\"")))
  ;; (is (= #inst "2010-11-12T13:14:15.666"
  ;;        (read-string "#inst\"2010-11-12T13:14:15.666\"")))
  ;; (is (= #uuid "550e8400-e29b-41d4-a716-446655440000"
  ;;        (read-string "#uuid \"550e8400-e29b-41d4-a716-446655440000\"")))
  ;; (is (= #uuid "550e8400-e29b-41d4-a716-446655440000"
  ;;        (read-string "#uuid\"550e8400-e29b-41d4-a716-446655440000\"")))
  (is (= (java.util.UUID/fromString "550e8400-e29b-41d4-a716-446655440000")
         (read-string "#uuid \"550e8400-e29b-41d4-a716-446655440000\"")))
  (is (= (java.util.UUID/fromString "550e8400-e29b-41d4-a716-446655440000")
                  (read-string "#uuid\"550e8400-e29b-41d4-a716-446655440000\"")))
  (when *default-data-reader-fn*
    (let [my-unknown (fn [tag val] {:unknown-tag tag :value val})]
      (is (= {:unknown-tag 'foo :value 'bar}
             (binding [*default-data-reader-fn* my-unknown]
               (read-string "#foo bar")))))))

(defrecord foo [])
(defrecord bar [baz buz])

(deftest read-record
  (is (= (foo.) (read-string "#clojure.tools.reader_test.foo[]")))
  (is (= (foo.) (read-string "#clojure.tools.reader_test.foo []"))) ;; not valid in clojure
  (is (= (foo.) (read-string "#clojure.tools.reader_test.foo{}")))
  (is (= (assoc (foo.) :foo 'bar) (read-string "#clojure.tools.reader_test.foo{:foo bar}")))

  (is (= (map->bar {}) (read-string "#clojure.tools.reader_test.bar{}")))
  (is (= (bar. 1 nil) (read-string "#clojure.tools.reader_test.bar{:baz 1}")))
  (is (= (bar. 1 nil) (read-string "#clojure.tools.reader_test.bar[1 nil]")))
  (is (= (bar. 1 2) (read-string "#clojure.tools.reader_test.bar[1 2]"))))

(deftest read-ctor
  (is (= "foo" (read-string "#java.lang.String[\"foo\"]"))))

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

    (are [re s opts] (is (thrown-with-msg? RuntimeException re (read-string opts s)))
         #"Feature should be a keyword" "#?((+ 1 2) :a)" opts
         #"even number of forms" "#?(:cljs :a :clj)" opts
         #"read-cond-splicing must implement" "(#?@(:clj :a))" opts
         #"is reserved" "(#?@(:foo :a :else :b))" opts
         #"must be a list" "#?[:foo :a :else :b]" opts
         #"Conditional read not allowed" "#?[:clj :a :default nil]" {:read-cond :BOGUS}
         #"Conditional read not allowed" "#?[:clj :a :default nil]" {}))
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
  (let [x (first (read-string {:read-cond :preserve} "(#?@(:clj [foo]))"))]
    (is (reader-conditional? x))
    (is (= x (reader-conditional '(:clj [foo]) true)))
    (is (:splicing? x))
    (is (= :foo (get x :no-such-key :foo)))
    (is (= (:form x) '(:clj [foo]))))
  (is (thrown-with-msg? RuntimeException #"No reader function for tag"
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
