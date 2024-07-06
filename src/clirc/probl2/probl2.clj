(ns clirc.probl2.probl2
  (:require [clojure.core.match :refer [match]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; RECURSOS AUXILIARES
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
"
Função auxiliar para criar código 

`result-var` : Nome da variável resultante das transformações
`cnd` : Condição a ser avaliada no IFF
`then` : trecho de código CLIRC para caso da condição ser verdadeira
`else` : trecho de código CLIRC para caso da condição ser falsa
"
(defn create-if 
  [result-var cnd then else]
  `((set! ~result-var (~(symbol "iff") ~cnd ~(if (nil? then) result-var then) ~(if (nil? else) result-var else)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; COMPOSIÇÃO DA ARVORE
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn expand-if-sttmt-arg
  [sttmt count-if option]
  (match [sttmt] 
    [(['set! lhs rhs] :seq)]
    (let [new-name (str lhs "$" option count-if)] 
      {lhs {:var new-name :sttmt `(set! ~new-name ~rhs)}})))

(defn expand-if-sttmt-args
  [sttmts count-if option]
  (let [aux (fn [acc sttmt]
             (merge acc (expand-if-sttmt-arg sttmt count-if option)))]
   (reduce aux {} sttmts)))

(defn expand-if-sttmt-tree
  [sttmt count-if]
  (match [sttmt]
    [(['if cnd then else] :seq)]
    (let [ex-then (expand-if-sttmt-args then count-if "then")
          ex-else (expand-if-sttmt-args else count-if "else")]
      {:num count-if,
       :cond cnd,
       :then ex-then,
       :else ex-else})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; TRADUÇÃO DA ARVORE EM CÓDIGO INTERMEDIÁRIO
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn translate-sttmt 
  [tree key vars]
  (letfn [(aux [acc sttmt-var]
               (concat acc [(get-in tree [key sttmt-var :sttmt])]))]
    (reduce aux () vars)))

(defn translate-if-sttmt 
  [tree vars]
  (letfn [(aux [acc sttmt-var]
               (let [then-var-name (get-in tree [:then sttmt-var :var])
                     else-var-name (get-in tree [:else sttmt-var :var])
                     cnd (get-in tree [:cond])]
                 (concat acc (create-if sttmt-var cnd then-var-name else-var-name))))]
    (reduce aux () vars)))

" 
 

 `tree`: 

 Retorno: 
"

(defn translate-if
  [tree]
  (let [then-vars (keys (get-in tree [:then])) 
        else-vars (keys (get-in tree [:else]))
        vars (distinct (concat then-vars else-vars))]
    (into [] (concat (translate-sttmt tree :then then-vars)
                     (translate-sttmt tree :else else-vars)
                     (translate-if-sttmt tree vars)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; EXECUÇÃO
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn expand-if
  [prog]
  (let [count-if 1
        aux (fn [acc sttmt]
              (let [tree (expand-if-sttmt-tree sttmt count-if)
                    sttmt_result (translate-if tree)]
                (into [] (concat acc sttmt_result))))]
       (reduce aux [] prog)))

