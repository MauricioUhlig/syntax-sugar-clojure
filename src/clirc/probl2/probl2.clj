(ns clirc.probl2.probl2
  (:require [clojure.core.match :refer [match]]))

(defn expand-if-sttmt-arg
  [sttmt count-if option]
  (match [sttmt] 
    [(['set! lhs rhs] :seq)]
    (let [new-name (str lhs "$" option count-if)] 
      {lhs {:var new-name :sttmt `(set! ~new-name ~rhs)}}
      )))

(defn expand-if-sttmt-args
  [sttmts count-if option]
  (let [aux (fn [acc sttmt]
             (merge acc (expand-if-sttmt-arg sttmt count-if option)))]
   (reduce aux {} sttmts)))

(defn expand-if-sttmt-tree
  [sttmt count-if]
  (match [sttmt]
    [(['iif cnd then else] :seq)]
    (let [ex-then (expand-if-sttmt-args then count-if "then")
          ex-else (expand-if-sttmt-args else count-if "else")]
    {:func 'if,
     :num count-if,
     :cond cnd,
     :then ex-then,
     :else ex-else})))

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
  [sttmt count-if]
  (let [tree-if (expand-if-sttmt-tree sttmt count-if)
        then-vars (keys (get-in tree-if [:then])) 
        else-vars (keys (get-in tree-if [:else]))
        vars (distinct (concat then-vars else-vars))]
    (into [] (concat (translate-sttmt tree-if :then then-vars)
                     (translate-sttmt tree-if :else else-vars)
                     (translate-if-sttmt tree-if vars)))))

(defn expand-if
  [prog]
  (let [count-if 1
        aux (fn [acc sttmt]
              (let [sttmt_result (expand-if-sttmt sttmt count-if)] 
                (into [] (concat acc sttmt_result))))]
       (reduce aux [] prog)))
