(ns clirc.probl2.probl2
  (:require [clojure.core.match :refer [match]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; RECURSOS AUXILIARES
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def count-if (atom 0))

(defn create-if
  [result-var cnd then else]
  `((set! ~result-var (if ~cnd ~(if (nil? then) result-var then) ~(if (nil? else) result-var else)))))

(defn get-tree-indexes
  [tree]
  (let [then (keys (get-in tree [:then]))
        else (keys (get-in tree [:else]))
        vars (remove #{:nested} (distinct (concat then else)))]
    {:then then
     :else else
     :vars vars}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; REGIÃO DE CRIAÇÃO DA AST
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; REGIÃO DE TRADUÇÃO
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(declare translate-if-sttmt-nested)
(defn translate-nested-sttmt
  [tree]
   (letfn [(aux [acc index]
             (concat acc [(translate-if-sttmt-nested (get-in tree [index]))]))]
     (reduce aux () (keys tree))))

(defn translate-if-cond
  [tree prev-cond-name]
  (let [cond-var-name (str prev-cond-name "$cond" (:num-if tree))
        not-cond-var-name (str "$not-cond" (:num-if tree))
        else-cond-var-name (str prev-cond-name "$not-cond" (:num-if tree))
        not-cond-var `(set! ~not-cond-var-name (~(symbol "not") ~(:cond tree)))
        ;; Se é o primeiro nivel, não tem condição anterior para agregar na geração do else
        else-cond-var (if (nil? prev-cond-name) nil `(set! ~else-cond-var-name (~(symbol "and") ~not-cond-var-name ~(:cond tree))))
        ;; Se é o primeiro nivel, não tem condição anterior. Por este motivo o AND retorna a identidade
        cond-var `(set! ~cond-var-name (~(symbol "and") ~(if (nil? prev-cond-name) (:cond tree) prev-cond-name) ~(:cond tree)))]
    {:sttmt `[~cond-var ~not-cond-var ~else-cond-var]
     :then cond-var-name
     :else else-cond-var-name}))

(defn translate-if-cond-nested
  [tree]
  (letfn [aux [acc tree index]
          (let [then (translate-if-cond (get-in tree [:then :nested index] (get-in tree [:cond])))
                else (translate-if-cond (get-in tree [:else :nested index] (get-in tree [:cond])))
                (into [] concat then else)])]
    (reduce aux [] tree (keys ())))
  )

(defn translate-sttmt 
  [tree key vars] 
  (letfn [(aux [acc sttmt-var]
               (concat acc [(cond (= sttmt-var :nested) (translate-nested-sttmt (get-in tree [key :nested])) 
                                  :else (get-in tree [key sttmt-var :sttmt]))]))]
    (reduce aux () vars)))

(defn translate-if-sttmt-nested
  [tree] 
  (let [indexes (get-tree-indexes tree)
        then (translate-sttmt tree :then (:then indexes))
        else (translate-sttmt tree :else (:else indexes))]
     (into [] (concat then else))))


(defn translate-if-helper 
  [tree vars] 
  (println vars)
  ;;(cond (not (some #{:nested} vars))
  (println (translate-if-cond tree nil))
        (letfn [(aux [acc sttmt-var] 
                     (cond sttmt-var(let [then-var-name (get-in tree [:then sttmt-var :var])
                           else-var-name (get-in tree [:else sttmt-var :var])
                           cnd (get-in tree [:cond])]
                       (concat acc (create-if sttmt-var cnd then-var-name else-var-name)))))]
          (reduce aux () vars)));;)

(defn translate-if 
  [tree] 
  (let [indexes (get-tree-indexes tree)] 
    ;;(println "nested "(translate-if-sttmt-nested tree))
     (into [] (concat (translate-if-sttmt-nested tree)
                      (translate-if-helper tree indexes)))))
  
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; REGIÃO DE EXECUÇÃO
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn expand-if-sttmt
  [sttmt]
  (let [tree (expand-if-sttmt-tree sttmt 0 0)]
        (translate-if tree)))

(defn expand-if
  [prog]
  (let [aux (fn [acc sttmt]
              (let [sttmt_result (expand-if-sttmt sttmt)] 
                (into [] (concat acc sttmt_result))))]
       (reduce aux [] prog)))
