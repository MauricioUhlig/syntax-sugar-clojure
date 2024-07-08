(ns clirc.probl4.probl4 
  (:require [clirc.probl4.probl4 :as tuples]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;; CLIRC-NAND to Tuple list
(defrecord VariableStore [inputs workspace outputs])

(defn create-variable-store []
  (->VariableStore (atom []) (atom []) (atom [])))

(defn parse-line [line]
  (let [lhs (second line)
        var1 (nth (nth line 2) 1)
        var2 (nth (nth line 2) 2)] 
    [lhs var1 var2]))

(defn add-variable [store var]
  (let [inputs (:inputs store)
        workspace (:workspace store)
        outputs (:outputs store)] 
    (cond (list? var)
          (cond
            (= (first var) :in) (when-not (some #(= % var) @inputs)
                                  (swap! inputs conj var))
            (= (first var) :out) (when-not (some #(= % var) @outputs)
                                   (swap! outputs conj var))
            :else (when-not (some #(= % var) @workspace)
                    (swap! workspace conj var)))
          :else (when-not (some #(= % var) @workspace)
                  (swap! workspace conj var)))))

(defn add-all-variables [store code]
  (reduce (fn [s line]
            (reduce (fn [s var]
                      (add-variable s var)
                      s)
                    s
                    (parse-line line)))
          store
          code))

(defn collect-variables [store]
  (let [inputs @(:inputs store)
        workspace @(:workspace store)
        outputs @(:outputs store)] 
    (concat (sort-by :in inputs) (sort workspace) (sort-by :out outputs))))

(defn generate-triples [variables code]
  (reduce (fn [acc line]
            (let [[lhs var1 var2] (parse-line line)]
              (conj acc [(.indexOf variables lhs)
                         (.indexOf variables var1)
                         (.indexOf variables var2)])))
          []
          code))

(defn clirc2tuples [code]
  (let [store (create-variable-store)]
    (add-all-variables store code)
    (let [variables (collect-variables store)]
      (println variables)
      [(count @(:inputs store)) (count @(:outputs store)) (generate-triples variables code)])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;; Run tuple list

(defn get-max-index
  [workspace] 
  (reduce (fn [max-value l]
            (let [local-max (apply max l)] 
              (cond (> local-max max-value) local-max
                    :else max-value)))
          0
          workspace))

(defn create-map
  [tuples]
  (let [max-index (get-max-index (nth tuples 2))]
    (reduce (fn[acc i] (assoc acc i 0)) {} (range (+ max-index 1)))))

(defn set-inputs
  [inputs tuple-map]
  (loop [acc tuple-map i (- (count inputs) 1) input inputs]
    (cond (seq input)
          (recur (assoc acc i (first input)) (dec i) (rest input))
          :else acc)))

(defn nand [a b](- 1 (* a b)))

(defn exec-tuples 
  [tuples-map tuples-code] 
  (reduce (fn [acc tuple-code] 
            (let [key (first tuple-code)
                  var1 (second tuple-code)
                  var2 (nth tuple-code 2)
                  result (nand (get acc var1) (get acc var2))]
              (assoc acc key result)))
          tuples-map
          tuples-code))

(defn get-ouput
  [result-map output-count]
  (let [mk (- (count result-map) 1)
        shift-left (- mk output-count)] 
    (loop [result [] i 0]
    (cond (< i output-count)
          (let [index (+ shift-left i)] 
            (recur (into [](concat [(get result-map index)] result)) (inc i)))
          :else result))))

(defn run-nand-tuples
  [tuples inputs]
  (cond (= (count inputs) (first tuples))
        (let [tuple-map (create-map tuples)
              tuple-map-inputs (set-inputs inputs tuple-map)
              result-map (exec-tuples tuple-map-inputs (nth tuples 2))] 
          (get-ouput result-map (second tuples))
          )
        :else (throw (ex-info "Wrong number of inputs values." {:inputs (count inputs) :expected (first tuples)}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;; Execute CLIR to Tuple list
(def code_clicr '[(set! temp0 (nand (:in 0) (:in 1)))
                  (set! temp1 (nand temp0 temp0))
                  (set! temp2 (nand (:in 2) temp1))
                  (set! (:out 0) (nand temp2 temp1))
                  (set! (:out 1) (nand temp0 temp1))
                  ])

(def tuple (clirc2tuples code_clicr))
(println (run-nand-tuples tuple [1 1 0]))