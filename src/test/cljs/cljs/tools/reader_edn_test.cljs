(ns cljs.tools.reader-edn-test
  (:require
    [cljs.test :as t :refer-macros [deftest is run-tests]]
    [cljs.tools.reader.edn :as edn]))

(defn inst [s] (js/Date. s))
(defn uuid [s] (cljs.core.UUID. s nil))

(def data-readers {'inst inst 'uuid uuid})

(deftest read-keyword
  (is (= :foo-bar (edn/read-string ":foo-bar")))
  (is (= :foo/bar (edn/read-string ":foo/bar")))
  (is (= :*+!-_? (edn/read-string ":*+!-_?")))
  (is (= :abc:def:ghi (edn/read-string ":abc:def:ghi")))
  (is (= :abc.def/ghi (edn/read-string ":abc.def/ghi")))
  (is (= :abc/def.ghi (edn/read-string ":abc/def.ghi")))
  (is (= :abc:def/ghi:jkl.mno (edn/read-string ":abc:def/ghi:jkl.mno")))
  (is (instance? cljs.core.Keyword (edn/read-string ":alphabet"))) )

(deftest read-tagged
  (is (= #inst "2010-11-12T13:14:15.666"
         (edn/read-string {:readers data-readers}
                          "#inst \"2010-11-12T13:14:15.666\"")))
  (is (= #inst "2010-11-12T13:14:15.666"
         (edn/read-string {:readers data-readers}
                          "#inst\"2010-11-12T13:14:15.666\"")))
  (is (= #uuid "550e8400-e29b-41d4-a716-446655440000"
         (edn/read-string {:readers data-readers}
                          "#uuid \"550e8400-e29b-41d4-a716-446655440000\"")))
  (is (= #uuid "550e8400-e29b-41d4-a716-446655440000"
         (edn/read-string {:readers data-readers}
                          "#uuid\"550e8400-e29b-41d4-a716-446655440000\"")))
  ;; (is (= (java.util.UUID/fromString "550e8400-e29b-41d4-a716-446655440000")
  ;;        (edn/read-string "#uuid \"550e8400-e29b-41d4-a716-446655440000\"")))
  ;; (is (= (java.util.UUID/fromString "550e8400-e29b-41d4-a716-446655440000")
  ;;        (edn/read-string "#uuid\"550e8400-e29b-41d4-a716-446655440000\"")))
  (let [my-unknown (fn [tag val] {:unknown-tag tag :value val})]
    (is (= {:unknown-tag 'foo :value 'bar}
           (edn/read-string {:default my-unknown} "#foo bar")))))
