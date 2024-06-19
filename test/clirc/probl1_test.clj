(ns clirc.probl1-test
  (:require [clojure.test :refer [are deftest is testing]]
            [clirc.core :refer :all]
            [clirc.probl1.probl1 :as cmp]))

(deftest test-eval-prog-aon-gt-comparer
  (testing "1 bit gt comparer - AON"
    (are [input output] (= (eval-prog-aon (cmp/cmp-n-bits-aon 1) input) output)
      [0 0] [0]
      [0 1] [0]
      [1 0] [1]
      [1 1] [0]))
  (testing "2 bits gt comparer - AON"
    (are [input output] (= (eval-prog-aon (cmp/cmp-n-bits-aon 2) input) output)
      [0 0 0 0] [0]
      [0 0 0 1] [0]
      [0 0 1 0] [0]
      [0 0 1 1] [0]
      [0 1 0 0] [1]
      [0 1 0 1] [0]
      [0 1 1 0] [0]
      [0 1 1 1] [0]
      [1 0 0 0] [1]
      [1 0 0 1] [1]
      [1 0 1 0] [0]
      [1 0 1 1] [0]
      [1 1 0 0] [1]
      [1 1 0 1] [1]
      [1 1 1 0] [1]
      [1 1 1 1] [0]))
  (testing "3 bits gt comparer - AON"
    (are [input output] (= (eval-prog-aon (cmp/cmp-n-bits-aon 3) input) output)
      [0 0 0 0 0 0] [0]
      [0 0 1 0 0 0] [1]
      [0 0 0 0 0 1] [0]
      [1 1 0 1 0 1] [1])))

(deftest test-eval-prog-nand-gt-comparer
  (testing "1 bit gt comparer - NAND"
    (are [input output] (= (eval-prog-nand (cmp/cmp-n-bits-nand 1) input) output)
      [0 0] [0]
      [0 1] [0]
      [1 0] [1]
      [1 1] [0]))
  (testing "2 bits gt comparer - NAND"
    (are [input output] (= (eval-prog-nand (cmp/cmp-n-bits-nand 2) input) output)
      [0 0 0 0] [0]
      [0 0 0 1] [0]
      [0 0 1 0] [0]
      [0 0 1 1] [0]
      [0 1 0 0] [1]
      [0 1 0 1] [0]
      [0 1 1 0] [0]
      [0 1 1 1] [0]
      [1 0 0 0] [1]
      [1 0 0 1] [1]
      [1 0 1 0] [0]
      [1 0 1 1] [0]
      [1 1 0 0] [1]
      [1 1 0 1] [1]
      [1 1 1 0] [1]
      [1 1 1 1] [0]))
  (testing "3 bits gt comparer - NAND"
    (are [input output] (= (eval-prog-nand (cmp/cmp-n-bits-nand 3) input) output)
      [0 0 0 0 0 0] [0]
      [0 0 1 0 0 0] [1]
      [0 0 0 0 0 1] [0]
      [1 1 0 1 0 1] [1])))