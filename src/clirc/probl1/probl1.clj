(ns clirc.probl1.probl1)

" Recebe um número N e retorna a string 'tempN' "
(defn create-temp [n] (symbol (str "temp" n)))

" Constantes "
(def aon_constant 5)
(def nand_constant 6)

" 
 Função auxiliar responsável por montar a estrutura da lista com o padrão de crescimento encontrado (da quantidade de operadores necessários)
 ao aumentar a quantidade de bits a serem comparados.

 `Total`: total de bits a serem comparados

 Retorno: lista no formato CLIRC-AON. E.g.: [(set! temp1 (not (:in 5)))
                                             (set! temp2 (and (:in 2) temp1))
                                             (set! temp3 (not (:in 4))) ...]
"
(defn cmp-n-bits-helper-aon
  [total]
  (loop [_atual 2 _total total _lista []]
  ;; Shift é utilizado para nomeação de variáveis temporárias
  ;; Digamos que atual seja 2 (ou seja, contabilizamos as duas operações da comparação entre 1 bit e outro)
  ;; As variáveis temp1 e temp2 estão criadas. Qual será a proxima?
  ;; O resultado do shift seria 2, então na hora de nomear, adicionamos progressivamente os números necessários
    (let [shift (+ (* (- _atual 2) aon_constant) 2)
          indiceA (- _total _atual)
          indiceB (- (* _total 2) _atual)
          result (into [] (concat _lista
                                  `[
                                    (set! ~(create-temp (+ shift 1)) (~(symbol "not") ~(list :in indiceB)))
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

`n` : Quantidade de bits dos números a serem comparados

Retorno:  lista no formato CLIRC. E.g.: [(set! temp1 (not (:in 5)))
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
                     ;; Outros bits serão comparados por essa função
                     (cmp-n-bits-helper-aon n)))))


" 
 Função auxiliar responsável por montar a estrutura da lista com o padrão de crescimento encontrado
 (da quantidade de operadores necessários) ao aumentar a quantidade de bits a serem comparados.

 `Total`: número de bits a serem comparados

 Retorno: lista no formato CLIRC-NAND. E.g.: [(set! temp1 (nand (:in 5)))
                                             (set! temp2 (nand (:in 2) temp1))
                                             (set! temp3 (nand (:in 4))) ...]
"
 (defn cmp-n-bits-helper-nand
   [total]
   (loop [_atual 2 _total total _lista []]
     (let [shift (+ (* (- _atual 2) nand_constant) 3)
           indiceA (- _total _atual)
           indiceB (- (* _total 2) _atual)
           result (into [] (concat _lista
                                `[(set! ~(create-temp (+ shift 1)) (~(symbol "nand") ~(list :in indiceB) ~(list :in indiceA))) 
                                   (set! ~(create-temp (+ shift 2)) (~(symbol "nand") ~(list :in indiceA) ~(list :in indiceA)))
                                   (set! ~(create-temp (+ shift 3)) (~(symbol "nand") ~(list :in indiceA) ~(create-temp (+ shift 2))))
                                   (set! ~(create-temp (+ shift 4)) (~(symbol "nand") ~(list :in indiceB) ~(create-temp (+ shift 2))))
                                   (set! ~(create-temp (+ shift 5)) (~(symbol "nand") ~(create-temp (+ shift 0)) ~(create-temp (+ shift 4)))) 
                                   (set! ~(create-temp (+ shift 6)) (~(symbol "nand") ~(create-temp (+ shift 3)) ~(create-temp (+ shift 5))))]))]
      ;; Verificação de parada/continuação
      (cond (= _atual _total)
            (into [] (concat result `[(set! ~(create-temp (+ shift 7)) (~(symbol "nand")  ~(create-temp (+ shift 6)) ~(create-temp (+ shift 6))))
                                      (set! ~(list :out 0) (~(symbol "nand") ~(create-temp (+ shift 7)) ~(create-temp (+ shift 7))))]))
            :else (recur (+ _atual 1) _total result)))))

"
Comparador de N bits com a função NAND.

`n` : Quantidade de bits dos números a serem comparados

Retorno:  lista no formato CLIRC-NAND. E.g.: [(set! temp1 (nand (:in 5) (:in 3)))
                                             (set! temp2 (nand (:in 2) temp1))
                                             (set! temp3 (nand (:in 4))) ...]
"
(defn cmp-n-bits-nand
  [n]
  (= n 1)
    '[(set! temp1 (nand (:in 0) (:in 1)))
      (set! temp2 (nand (:in 0) temp1)) 
      (set! (:out 0) (nand temp2 temp2))]
    :else
    (into [] (concat `[
                       (set! ~(symbol "temp1") (~(symbol "nand") ~(list :in (- (* n 2) 1)) ~(list :in (- n 1))))
                       (set! ~(symbol "temp2") (~(symbol "nand") ~(list :in (- n 1)) ~(symbol "temp1")))
                       (set! ~(symbol "temp3") (~(symbol "nand") ~(list :in ~(symbol "temp2")) ~(symbol "temp2")))
                       
                       ]
                     (cmp-n-bits-helper-nand n)))
  )