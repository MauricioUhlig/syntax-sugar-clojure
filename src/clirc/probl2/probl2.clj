(ns clirc.probl2.probl2
  (:require [clojure.core.match :refer [match]]))

(def count-if (atom 0))
(defn expand-if-sttmt-arg
  [sttmt count-if option]
  (match [sttmt] 
    [(['set! lhs rhs] :seq)]
    (let [new-name (str lhs "$" option count-if)] 
      {lhs {:var new-name :sttmt `(set! ~new-name ~rhs)}}
      )))

;; Preciso da declaração dele antes, pois terei dois metodos recorrentes
(declare expand-if-sttmt-tree)

(defn expand-if-sttmt-nested
  [sttmt count-if option num-block index]
  (let [nested (expand-if-sttmt-tree sttmt (inc num-block) index)]
    (cond (nil? nested) (expand-if-sttmt-arg sttmt count-if option)
          :else {index nested})))

(defn assoc-or-merge
  [acc sttmt-tree]
  (let [key (first (keys sttmt-tree))] 
    (cond (number? key) (assoc-in acc [:nested key] (get-in sttmt-tree [key]))
          :else (merge acc sttmt-tree))))

(defn expand-if-sttmt-args
  [sttmts count-if option num-block]
  (let [index (atom -1)
        aux (fn [acc sttmt]
              (swap! index inc) 
             (let [nested-result (expand-if-sttmt-nested sttmt count-if option num-block @index)]
               (assoc-or-merge acc nested-result)))]
   (reduce aux {} sttmts)))

(defn expand-if-sttmt-tree
  [sttmt num-block index]
  (match [sttmt]
    [(['iif cnd then else] :seq)]
    (do (swap! count-if inc) ;; Incrementa o contador de IFs
        (let [num-if @count-if
              ex-then (expand-if-sttmt-args then num-if "then" num-block)
              ex-else (expand-if-sttmt-args else num-if "else" num-block)]
          {:deep num-block,
           :index index,
           :cond cnd,
           :num-if num-if,
           :then ex-then,
           :else ex-else}))
    :else nil))

(defn translate-sttmt 
  [tree key vars]
  (letfn [(aux [acc sttmt-var]
               (concat acc [(get-in tree [key sttmt-var :sttmt])]))]
    (reduce aux () vars)))

(defn create-if 
  [result-var cnd then else]
  `((set! ~result-var (if ~cnd ~(if (nil? then) result-var then) ~(if (nil? else) result-var else)))))

(defn translate-if-sttmt 
  [tree vars] 
  (letfn [(aux [acc sttmt-var]
               (let [then-var-name (get-in tree [:then sttmt-var :var])
                     else-var-name (get-in tree [:else sttmt-var :var])
                     cnd (get-in tree [:cond])]
                 (concat acc (create-if sttmt-var cnd then-var-name else-var-name))))]
    (reduce aux () vars)))

(defn expand-if-sttmt
  [sttmt]
  (let [tree-if (expand-if-sttmt-tree sttmt 0 0)
        then-vars (keys (get-in tree-if [:then])) 
        else-vars (keys (get-in tree-if [:else]))
        vars (distinct (concat then-vars else-vars))]
    (println tree-if)
    (into [] (concat (translate-sttmt tree-if :then then-vars)
                     (translate-sttmt tree-if :else else-vars)
                     (translate-if-sttmt tree-if vars)))))

(defn expand-if
  [prog]
  (let [aux (fn [acc sttmt]
              (let [sttmt_result (expand-if-sttmt sttmt)] 
                (into [] (concat acc sttmt_result))))]
       (reduce aux [] prog)))
