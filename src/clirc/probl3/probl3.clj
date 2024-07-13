(ns clirc.probl3.probl3
  (:require [clojure.core.match :refer [match]]))

;; entender como um for que caminha sobre o indice de B, multiplicando por A. Guardando o resultado em OUT. Preciso também carregar comigo o carry da operação
;; pois ao multiplicar, já posso resolver a soma e carry, antes de guardar em out
;; lembrar do ultimo carry que soma com o 0 do bit 0 de saida

(def cmd "(set! a (and (:in 0) (:in 1)))")

;; Vai ajudar pra não escrever tudo com quote e unquote '(~())
;;(println  (read-string cmd))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;; CLIRC-FOR
(defn create-var [a i] (symbol (str a i)))

;; 
(defn exp-index-replace
  [exp index]
  (letfn [(aux [acc item]
               (cond (= item (symbol "i"))(concat acc [index])
                     :else (concat acc [item])))]
    (reduce aux [] exp)))

(defn exp-index [exp index] 
  (cond (symbol? exp) index
        (number? exp) exp
        :else (eval (exp-index-replace exp index))))

(defn expand-ref-exp
  [ref]
  (cond (list? ref) (second ref)
        :else ref))

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
    :else arg
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; CLIRC-PROC

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Montando estrutura de dados a partir do código de entrada
(declare expand-proc-return
         expand-proc-input-args
         expand-proc-args
         expand-proc-funcs
         expand-proc-body)

(defn expand-proc-tree
  [proc]
  (match [proc]
    [(['proc funcname args & body] :seq)]
    (let [in-args (expand-proc-input-args args)]
      {:name funcname
       :in in-args
       :body-sttmts (expand-proc-body body in-args)
       :return (expand-proc-return body in-args)})
    :else nil))

(defn expand-proc-return
  [body input-args]
  (letfn [(aux [acc sttmt]
            (match [sttmt]
              [(['return func] :seq)]
              (expand-proc-funcs nil func input-args)
              :else acc))]
    (reduce aux nil body)))

(defn expand-proc-body
  [body input-args]
  (letfn [(aux [acc sttmt]
            (match [sttmt]
              [(['set! var func] :seq)]
              (into [] (concat acc [(expand-proc-funcs var func input-args)]))
              :else acc))]
    (reduce aux [] body)))

(defn expand-proc-input-args [args] (map #(keyword %) args))
(defn expand-proc-funcs [var func input-args] {:var (first (expand-proc-args [var] input-args)) :func (first func) :args (expand-proc-args (rest func) input-args)})

(defn expand-proc-args
  [func-args input-args]
  (letfn [(aux [acc func-arg]
            (cond (some #{(keyword func-arg)} input-args)
                  (concat acc [(keyword func-arg)])
                  :else (concat acc (list func-arg))))]
    (reduce aux [] func-args)))


(defn expand-proc
  [prog]
  (let [tree (letfn [(aux [acc proc]
                       (let [proc-tree (expand-proc-tree proc)]
                         (cond (nil? proc-tree) acc
                               :else (concat acc [proc-tree]))))]
               (reduce aux [] prog))]
    tree))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Gerando código clirc sem sintax sugar de proc
(def counter (atom 0))
(defn count-inc [] (swap! counter inc))

(declare bind-in-args
         handle-proc
         handle-proc-get-func-tree
         handle-proc-func-tree
         handle-proc-func-tree-body-sttmts
         handle-proc-func-tree-return
         bind-func-args
         create-sttmt)

(defn handle-proc
  ([prog]
   (handle-proc prog (expand-proc prog)))
  ([prog proc-tree]
   ;;(println proc-tree)
   (loop [sttmts prog]
     (let [exec (letfn [(aux [acc sttmt]
                          (match [sttmt]
                            [(['set! var func] :seq)]
                            (let [func-tree (handle-proc-get-func-tree (first func) proc-tree)]
                              (cond (nil? func-tree) {:sttmts (into [] (concat (:sttmts acc) [sttmt])) :has-proc? (:has-proc? acc)}
                                    :else (do
                                            (count-inc) ;; só incrementa quando existe um procedimento para abrir na arvore
                                            {:sttmts (into [] (concat (:sttmts acc) (handle-proc-func-tree var func func-tree)))
                                             :has-proc? true})))
                            :else nil))]
                  (reduce aux {:sttmts [] :has-proc? false} sttmts))]
       (cond (true? (:has-proc? exec)) (recur (:sttmts exec))
             :else (:sttmts exec))))))

(defn handle-proc-get-func-tree
  [func-name proc-tree]
  (loop [tree proc-tree]
    (cond (empty? tree) nil
          (= (:name (first tree)) func-name) (first tree)
          :else (recur (rest tree)))))

(defn bind-in-args
  [func-name in-args proc-args]
  ;;(println "bind-in-args" func-name in-args proc-args)
  (cond (not= (count in-args) (count proc-args))
        (throw (ex-info "Wrong number of arguments." {:fname func-name :in-args in-args :proc-args proc-args}))
        (empty? in-args) nil
        :else (loop [acc {} in-arg' in-args proc-args' proc-args]
                (cond (empty? in-arg') acc
                      :else (recur (assoc-in acc [(first proc-args')] (first in-arg')) (rest in-arg') (rest proc-args'))))))

(defn create-var-name
  [func var]
  (symbol (str func @counter "$" var)))

(defn handle-proc-func-tree
  [var func tree]
  (let [func-args  (rest func)
        binded-args (bind-in-args (first func) func-args (:in tree))
        body-sttmt (handle-proc-func-tree-body-sttmts binded-args tree)
        return (handle-proc-func-tree-return var tree binded-args)]
    ;;(println "binded-args" (:name tree) binded-args)
    (concat body-sttmt return)))

(defn handle-proc-func-tree-return
  [var tree binded-args]
  `((set! ~var (~(get-in tree [:return :func]) ~@(bind-func-args (:name tree) binded-args (get-in tree [:return :args]))))))

(defn handle-proc-func-tree-body-sttmts
  [binded-args tree]
  (letfn [(aux [acc body-sttmt]
            (concat acc (create-sttmt (:name tree) (:func body-sttmt) (:var body-sttmt) (:args body-sttmt) binded-args)))]
    (reduce aux [] (:body-sttmts tree))))

(defn create-sttmt
  [func func-name var func-args binded-args]
  `((set! ~@(bind-func-args func binded-args [var]) (~func-name ~@(bind-func-args func binded-args func-args)))))

(defn bind-func-args
  [func bind-in-args func-args]
  ;;(println func bind-in-args func-args)
  (letfn [(aux [acc func-arg]
            (concat acc [(cond (keyword? func-arg) (get-in bind-in-args [func-arg])
                               (list? func-arg) func-arg
                               :else (create-var-name func func-arg))]))]
    (reduce aux [] func-args)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Execução para teste
(def proc '[(proc full-adder [a b cin cout]
                  (set! xor1 (xor a b))
                  (set! and1 (and xor1 cin))
                  (set! and2 (and a b))
                  (set! cout (or and1 and2))
                  (return (xor xor1 cin)))
            (proc xor [a b]
                  (set! or1 (or a b))
                  (set! and1 (and a b))
                  (set! nand1 (not and1))
                  (return (and or1 nand1)))

            (set! soma (full-adder (:in 0) (:in 1) (:out 0) (:out 1)))])

(def tree (expand-proc proc))
;;(println (handle-proc proc tree))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def procs '[(proc calc-result-carry [carry-prev mult1 result-out]
                   (set! xor1 (xor carry-prev mult1))
                   (set! and2 (and xor1 result-out))
                   (set! result-out (xor xor1 result-out))
                   (set! and1 (and carry-prev mult1))
                   (return (or and1 and2)))
             (proc xor [a b]
                   (set! or1 (or a b))
                   (set! and1 (and a b))
                   (set! nand1 (not and1))
                   (return (and or1 nand1)))
             (proc ZERO [a]
                   (set! nota (not a))
                   (return (and a nota)))
             (proc mult [A B carry-prev result-out]
                   (set! mult1 (and A B))
                   (return (calc-result-carry carry-prev mult1 result-out)))
             (proc identidade [a] (return (and a a)))])

(def for-3-bits '[(for [i 0 5]
                    (set! (:out (:ref i)) (ZERO (:in (:ref i))))) ;; atribuindo constante 0
                  (for [i 0 2]
                    (set! (:var carry0$ i) (ZERO (:in (:ref i))));;(identidade (:out (:ref (- 3 i))))) ;; garantindo a existencia do primeiro carry
                    (set! multiplicador (identidade (:in (:ref (- 5 i)))))
                    (set! (:var carry1$ i);; Salva o resultado da multiplicação no bit menos significativo
                          (mult (:in 2) ;; Primeiro termo
                                multiplicador ;; Segundo termpo
                                (:var carry0$ i) ;; Carry da operação anterior
                                (:out (:ref (- 5 i))))) ;; Onde o resultado da operação atual será armazenado

                    (set! (:var carry2$ i) ;; Salva o resultado da multiplicação no bit menos significativo
                          (mult (:in 1) ;; Primeiro termo
                                multiplicador ;; Segundo termpo
                                (:var carry1$ i) ;; Carry da operação anterior
                                (:out (:ref (- 4 i))))) ;; Onde o resultado da operação atual será armazenado

                    (set! (:var carry3$ i) ;; Salva o resultado da multiplicação no bit menos significativo
                          (mult (:in 0) ;; Primeiro termo
                                multiplicador ;; Segundo termpo
                                (:var carry2$ i) ;; Carry da operação anterior
                                (:out (:ref (- 3 i))))) ;; Onde o resultado da operação atual será armazenado

                    ;; Atribui o ultimo carry ao ultimo bit de outpub
                    (set! (:out (:ref (- 2 i))) (identidade (:var carry3$ i))))])

(def proc-for-3-bits (into [] (concat procs for-3-bits)))

(def proc-3-bits (expand-for proc-for-3-bits))
(def proc-code-3 (handle-proc proc-3-bits))
(println proc-code-3)

;; Executar no arquivo core.clj
;;(println (eval-prog-aon f4r/proc-code-3 [1 1 1 , 1 1 1]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;; Gerador de Multiplicador para N-bits
(declare multi-N-bits-set-zero
         create-mult-N-bits
         multi-N-bits-for-header-str
         multi-N-bits-for-body-str
         multi-N-bits-for-full-body)
(defn mult-N-bits-clirc 
  [n]
  (let [mult-with-sugar (create-mult-N-bits n)
        expanded-for (expand-for mult-with-sugar)]
    (handle-proc expanded-for)))
(defn create-mult-N-bits
  [n]
  (into [] (concat
            procs
            (multi-N-bits-set-zero n)
            (multi-N-bits-for-full-body n))))

(defn multi-N-bits-set-zero [n] (read-string (format "[(for [i 0 %s](set! (:out (:ref i)) (ZERO (:in (:ref i)))))]" (- (* n 2) 1))))

(defn multi-N-bits-for-header-str
  [n]
  (format "[(for [i 0 %1$d]
          (set! (:var carry0$ i) (ZERO (:in (:ref i))))
          (set! multiplicador (identidade (:in (:ref (- %3$d i)))))" (- n 1) n (- (* n 2) 1)))
(defn multi-N-bits-for-mult-sttmt-str
  [i bit-a bit-b]
  (str "(set! (:var carry" (inc i) "$ i)
              (mult (:in " bit-a ")
                    multiplicador
                    (:var carry" i "$ i)
                    (:out (:ref (- " bit-b" i)))))"))
(defn multi-N-bits-last-carry [n] (str "(set! (:out (:ref (- "(- n 1)" i))) (identidade (:var carry"n"$ i))))]" ))

(defn multi-N-bits-for-body-str 
  [n]
  (loop [i 0 bit-a (- n 1) bit-b (- (* n 2) 1) acc ""]
    (cond (= i n) acc
          :else (recur
                 (inc i)
                 (dec bit-a)
                 (dec bit-b)
                 (str acc (multi-N-bits-for-mult-sttmt-str i bit-a bit-b))))))
(defn multi-N-bits-for-full-body
  [n]
  (let [header (multi-N-bits-for-header-str n)
        body (multi-N-bits-for-body-str n)
        carry (multi-N-bits-last-carry n)
        full-for-str (str header body carry)] 
    (read-string full-for-str)))

;;;; Execução 
;;(println (mult-N-bits-clirc 3)) ;; Gerando código em CLICR padrão para calcular a multiplicação de numeros de 3 bits
;; Executar no arquivo core.clj
;;(println "mult-N-bits sem sintax sugar" (f4r/mult-N-bits-clirc 4))
;;(println "Resultado multiplicação de [1 1 1 1] x [1 1 1 1] =" (eval-prog-aon (f4r/mult-N-bits-clirc 4) [1 1 1 1 , 1 1 1 1]))