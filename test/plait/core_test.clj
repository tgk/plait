(ns plait.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [plait.core :as sut :refer [plait]]))

(deftest plait-test
  (plait [a 42
          b (inc a)]
    (testing "works like a let"
      (is (= b 43)))
    (plait [b (+ 2 a)]
      (testing "replaces last binding"
        (is (= b 44))))
    (plait [a 10]
      (testing "replaces first binding")
      (is (= b 11))))
  (testing "does not overwrite _ symbols"
    (plait [a (atom 0)
            _ (swap! a inc)]
      (plait [_ 42]
        (is (= 1 @a)))))
  (testing "recurs on a do"
    (plait [a 42
            b (inc a)]
      (do
        (plait [b (+ 2 a)]
          (is (= b 44)))))))
