(ns clirc.probl3.probl3
  (:require [clojure.core.match :refer [match]]))

;; entender como um for que caminha sobre o indice de B, multiplicando por A. Guardando o resultado em OUT. Preciso também carregar comigo o carry da operação
;; pois ao multiplicar, já posso resolver a soma e carry, antes de guardar em out
;; lembrar do ultimo carry que soma com o 0 do bit 0 de saida

(def cmd "(set! a (and (:in 0) (:in 1)))")

;; Vai ajudar pra não escrever tudo com quote e unquote '(~())
;;(println  (read-string cmd))

(defn create-var [a i] (str a i))

(defn exp-index [exp index] 
  (cond (symbol? exp) index
        :else (eval (list (first exp) index (nth exp 2)))))
(defn expand-ref-exp [ref] (second ref))

(declare expand-for-loop
         expand-for-body
         expand-for-body-var
         expand-for-body-func
         expand-for-body-var-list)

(defn expand-for 
  [prog]
  (letfn [(expand-for-acc [acc sttmt]
            (match [sttmt]
              [(['for loop-range & body] :seq)]
              (concat acc (expand-for-loop loop-range body))
              :else (concat acc [sttmt])))]
    (reduce expand-for-acc [] prog)))

(defn expand-for-loop
  [loop-range body]
  (let [[_ start end] loop-range]
    (letfn [(aux [acc index]
               (concat acc (expand-for-body index body)))]
    (reduce aux [] (range start (inc end)) ))))


(defn expand-for-body 
  [index body] 
  (letfn [(expand-for-body-acc [acc sttmt]
                           (match [sttmt]
                             [(['set! lhs rhs] :seq)](let [var (expand-for-body-var lhs index)
                                                           func (expand-for-body-func rhs index)] 
                                                       (concat acc `((set! ~var ~func))))))]
    (reduce expand-for-body-acc [] body)))

(defn expand-for-body-var
  [arg index] 
  (match [arg]
    [([':var name exp] :seq)] (create-var name (exp-index exp index))
    [([':in ref] :seq)] `(:in ~(exp-index (expand-ref-exp ref) index))
    [([':out ref] :seq)] `(:out ~(exp-index (expand-ref-exp ref) index))
    ))

(defn expand-for-body-func
  [funcall index] 
  (match [funcall]
    [([func & args] :seq)] `(~func ~@(expand-for-body-var-list args index))))

(defn expand-for-body-var-list
  [args index]
  (letfn [(aux [acc arg] 
               (concat acc [(expand-for-body-var arg index)]))]
    (reduce aux [] args)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def code_for '[(set! carry0 (zero (:in 0)))
                (for [i 0 2]
                  (set! (:var temp i) (xor (:in (:ref i)) (:in (:ref (+ i 3)))))
                  (set! (:out (:ref i)) (xor (:var temp i) (:var carry i)))
                  (set! (:var a i) (and (:in (:ref i)) (:in (:ref (+ i 3)))))
                  (set! (:var b i) (and (:var temp i) (:var carry i)))
                  (set! (:var carry (+ i 1)) (or (:var a i) (:var b i))))
                (set! (:out 3) (and carry3 carry3))])

(println (expand-for code_for))