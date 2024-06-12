(ns clirc.probl1.probl1)

" Recebe um número N e retorna a string 'tempN' "
(defn create-temp [n] (symbol (str "temp" n)))

" Constantes "
(def aon_constant 5)
(def nand_constant 6)

" 
 Função recursiva auxiliar  responsável por montar a estrutura da lista com o padrão de crescimento encontrado (da quantidade de operadores necessários)
 ao aumentar a quantidade de bits a serem comparados.

`Atual` : A 'base', quantos operadores já foram contados. A primeira chamada deverá ter o número 2 nesse parâmetro, pois ao comparar
 2 números de um bit, são necessários apenas 2 operadores. O padrão é observado a partir da comparação de 2+ bits

 Total : total de bits a serem comparados

 Retorno: lista no formato CLIRC. E.g.: [(set! temp1 (not (:in 5)))
                                             (set! temp2 (and (:in 2) temp1))
                                             (set! temp3 (not (:in 4))) ...]
"
(defn cmp-n-bits-helper-aon
  [atual total]
  (loop [_atual atual _total total _lista []]
  ;; Shift é utilizado para nomeação de variáveis temporárias
;; Digamos que atual seja 2 (ou seja, contabilizamos as duas operações da comparação entre 1 bit e outro)
;; As variáveis temp1 e temp2 estão criadas. Qual será a proxima?
;; O resultado do shift seria 2, então na hora de nomear, adicionamos progressivamente os números necessários

    (let [shift (+ (* (- _atual 2) aon_constant) 2)
          indiceA (- _total _atual)
          indiceB (- (* _total 2) _atual)
          result (into [] (concat _lista
                                  `[(set! ~(create-temp (+ shift 1)) (~(symbol "not") ~(list :in indiceB)))
                                    (set! ~(create-temp (+ shift 2)) (~(symbol "and") ~(list :in indiceA) ~(create-temp (+ shift 1))))
                                    (set! ~(create-temp (+ shift 3)) (~(symbol "or") ~(list :in indiceA) ~(create-temp (+ shift 1))))
                                    (set! ~(create-temp (+ shift 4)) (~(symbol "and") ~(create-temp (+ shift 0)) ~(create-temp (+ shift 3))))
                                    (set! ~(create-temp (+ shift 5)) (~(symbol "or") ~(create-temp (+ shift 4)) ~(create-temp (+ shift 2))))]))]
      ;; Verificação de parada/continuação
      (cond (= _atual _total)
            (into [] (concat result `[(set! ~(list :out 0) (~(symbol "and") ~(create-temp (+ shift 5)) ~(create-temp (+ shift 5))))]))
            :else (recur (+ _atual 1) _total result)))))

"
Comparador de N bits com funções AON.

n : Quantidade de bits dos números a serem comparados

Retorno :  lista no formato CLIRC. E.g.: [(set! temp1 (not (:in 5)))
                                             (set! temp2 (and (:in 2) temp1))
                                             (set! temp3 (not (:in 4))) ...]
"
(defn cmp-n-bits-aon
  [n]
  (cond
    (= n 1)
    '[(set! temp1 (not (:in 1)))
      (set! temp2 (and (:in 0) temp1))
      (set! (:out 0) (and temp2 temp2))]
    :else
    (into [] (concat `[;; Comparador do bit menos significativo
                       ;; in contém as posições do array de input, se o input é [0 0 0 1 1 1], (in: 3) seria input[3] = 1
                       (set! ~(symbol "temp1") (~(symbol "not") ~(list :in (- (* n 2) 1))))
                       (set! ~(symbol "temp2") (~(symbol "and") ~(list :in (- n 1)) ~(symbol "temp1")))]
                     (cmp-n-bits-helper-aon 2 n)))))



; In Progress
; 010 010
; (defn cmp-n-bits-helper-nand
;   3       4
;   [atual total]
;   (loop [_atual atual _total total _lista []]
;     shift = 3
;     (let [shift (+ (* (- _atual 3) nand_constant) 3)
;           1
;           indiceA (- _total _atual)
;           3
;           indiceB (- (* _total 2) _atual)
;           result (into [] (concat _lista
                                 ; `[(set! ~(create-temp (+ shift 1)) (~(symbol "nand") ~(list :in indiceB)))
                                 ;   (set! ~(create-temp (+ shift 2)) (~(symbol "nand") ~(list :in indiceA) ~(create-temp (+ shift 1))))
                                 ;   (set! ~(create-temp (+ shift 3)) (~(symbol "nand") ~(list :in indiceA) ~(create-temp (+ shift 1))))
                                 ;   (set! ~(create-temp (+ shift 4)) (~(symbol "nand") ~(create-temp (+ shift 0)) ~(create-temp (+ shift 3))))
                                 ;   (set! ~(create-temp (+ shift 5)) (~(symbol "nand") ~(create-temp (+ shift 4)) ~(create-temp (+ shift 2))))
                                 ;   (set! ~(create-temp (+ shift 6)) (~(symbol "nand") ~(create-temp (+ shift 4)) ~(create-temp (+ shift 2))))]))]
      ;; Verificação de parada/continuação
      ;(cond (= _atual _total)
            ;(into [] (concat result `[(set! ~(list :out 0) (~(symbol "nand") ~(create-temp (+ shift 5)) ~(create-temp (+ shift 5))))]))
           ; :else (recur (+ _atual 1) _total result)))))

;; In progress
;(defn cmp-n-bits-nand
;  [n]
;  (= n 1)
;    '[(set! temp1 (nand (:in 0) (:in 1)))
;      (set! temp2 (nand (:in 0) temp1)) 
;      (set! (:out 0) (nand temp2 temp2))]
;    :else
;    (into [] (concat `[;; Comparador do bit menos significativo
;                       ;; in contém as posições do array de input, se o input é [0 0 0 1 1 1], (in: 3) seria input[3] = 1
;                       (set! ~(symbol "temp1") (~(symbol "not") ~(list :in (- (* n 2) 1))))
;                       (set! ~(symbol "temp2") (~(symbol "and") ~(list :in (- n 1)) ~(symbol "temp1")))]
;                     (cmp-n-bits-helper-nand 3 (+ n 1))))
;  )