(ns clirc.probl1.probl1)

(defn create-temp [n] (symbol (str "temp" n)))


(defn cmp-n-bits-helper
  [atual total]
  (loop [_atual atual _total total _lista []]
    (let [shift (+ (* (- _atual 2) 5) 2)
          indiceA (- _total _atual)
          indiceB (- (* _total 2) _atual)
          result (into [] (concat _lista
                                  `[(set! ~(create-temp (+ shift 1)) (~(symbol "not") ~(list :in indiceB)))
                                    (set! ~(create-temp (+ shift 2)) (~(symbol "and") ~(list :in indiceA) ~(create-temp (+ shift 1))))
                                    (set! ~(create-temp (+ shift 3)) (~(symbol "or") ~(list :in indiceA) ~(create-temp (+ shift 1))))
                                    (set! ~(create-temp (+ shift 4)) (~(symbol "and") ~(create-temp (+ shift 0)) ~(create-temp (+ shift 3))))
                                    (set! ~(create-temp (+ shift 5)) (~(symbol "or") ~(create-temp (+ shift 4)) ~(create-temp (+ shift 2))))]))]
      (cond (= _atual _total)
            (into [] (concat result `[(set! ~(list :out 0) (~(symbol "and") ~(create-temp (+ shift 5)) ~(create-temp (+ shift 5))))]))
            :else (recur (+ _atual 1) _total result)))))

(defn cmp-n-bits
  [n]
  (cond
    (= n 1)
    '[(set! temp1 (not (:in 1)))
      (set! temp2 (and (:in 0) temp1))
      (set! (:out 0) (and temp2 temp2))]
    :else
    (into [] (concat `[;; Comparador do bit menos significativo
                       (set! ~(symbol "temp1") (~(symbol "not") ~(list :in (- (* n 2) 1))))
                       (set! ~(symbol "temp2") (~(symbol "and") ~(list :in (- n 1)) ~(symbol "temp1")))]
                     (cmp-n-bits-helper 2 n)))))