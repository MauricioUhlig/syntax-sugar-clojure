(ns clirc.probl4.probl4 
  (:require [clirc.probl4.probl4 :as tuples]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;; CLIRC-NAND to Tuple list
(defrecord VariableStore [inputs workspace outputs])

(defn create-variable-store []
  (->VariableStore (atom []) (atom []) (atom [])))

(defn parse-line 
  "Método responsável por ler o conteúdo da linha (statement de atribuição) e mapeia as variaveis existentes
   
   `lhs`: é a variável que recebe a atribuição
   
   `var1` e `var2`: são as variáveis de input do `nand`
   
   O retorno é uma lista contendo as 3 variáveis [lhs var1 var2]"
  [line] 
  (let [lhs (second line)
        var1 (nth (nth line 2) 1)
        var2 (nth (nth line 2) 2)] 
    [lhs var1 var2]))

(defn add-variable 
  "Método responsável por receber um dicionário de variáveis `store` e uma variável `var`.
   Sua lógica de operação é verificar se a variável `var` existe dentro do dicionário `store`.
   Se existe, nada acontece, senão o valor é armazenado em sua respectiva chave no dicionário `store`"
  [store var]
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

(defn add-all-variables 
  "Método responsável por executar o parser `parse-line` e salvar as variáveis no dicionário com o método `add-variable`
   
   O retorno é o dicionário `store` preenchido com todas as variáveis existentes no código CLIRC de input"
  [store code]
  (reduce (fn [s line]
            (reduce (fn [s var]
                      (add-variable s var)
                      s)
                    s
                    (parse-line line)))
          store
          code))

(defn collect-variables 
  "Método responsável por retornar a lista de todas as váriaveis existentes no dicionário de forma ordenada por tipo `inputs workspace ouputs`"
  [store]
  (let [inputs @(:inputs store)
        workspace @(:workspace store)
        outputs @(:outputs store)] 
    (concat (sort-by :in inputs) (sort workspace) (sort-by :out outputs))))

(defn generate-triples 
  "Método responsável por executar o parser do código, retornando as variáveis e procurando estas na lista de variáveis existentes.
   Retorna uma lista de indices das variavis, sendo o resultado uma tupla"
  [variables code]
  (reduce (fn [acc line]
            (let [[lhs var1 var2] (parse-line line)]
              (conj acc [(.indexOf variables lhs)
                         (.indexOf variables var1)
                         (.indexOf variables var2)])))
          []
          code))

(defn clirc2tuples 
  "Método que recebe o código em CLIRC padrão e transforma em uma lista de tuplas
   
   `code`: Programa em CLIRC padrão
   
   Retorna lista de tuplas. Ex.: [1 1 [[1 0 2] [2 1 3]...]]"
  [code] 
  (let [store (create-variable-store)]
    (add-all-variables store code)
    (let [variables (collect-variables store)]
      [(count @(:inputs store)) (count @(:outputs store)) (generate-triples variables code)])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;; Run tuple list

(defn get-max-index
  "Busca o maior indice existente nas tuplas - Equivalente a saber quantas variáveis existem"
  [workspace] 
  (reduce (fn [max-value l]
            (let [local-max (apply max l)] 
              (cond (> local-max max-value) local-max
                    :else max-value)))
          0
          workspace))

(defn create-map
  "Cria um dicionário, atribuindo valor 0 para todas as variáveis possíveis no conjunto de tuplas informado"
  [tuples]
  (let [max-index (get-max-index (nth tuples 2))]
    (reduce (fn[acc i] (assoc acc i 0)) {} (range (+ max-index 1)))))

(defn set-inputs
  "Atribui o valor do conjunto de input nas respectivas variáveis no dicionário de trabalho"
  [inputs tuple-map]
  (loop [acc tuple-map i (- (count inputs) 1) input inputs]
    (cond (seq input)
          (recur (assoc acc i (first input)) (dec i) (rest input))
          :else acc)))

(defn nand [a b](- 1 (* a b)))

(defn exec-tuples 
  "Executa os conjuntos de tuplas, aplicando `nand` nas variaveis das posições `1` e `2` de cada tupla, gravando no dicionário no indice da posição `0` da tupla"
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
  "Retorna a lista de variáveis `output`, calculando o indice baseado na quantidade de váriaveis totais e variáveis `ouput`"
  [result-map output-count]
  (let [mk (- (count result-map) 1)
        shift-left (- mk output-count)] 
    (loop [result [] i 0]
    (cond (< i output-count)
          (let [index (+ shift-left i)] 
            (recur (into [](concat [(get result-map index)] result)) (inc i)))
          :else result))))

(defn run-nand-tuples
  "Método de execução que recebe as tuplas e os bits de inputs.
   
   Retorna o resultado da execução das tuplas sobre os inputs."
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

;(def tuple (clirc2tuples code_clicr))
;(println "tuplas " tuple)
;(println (run-nand-tuples tuple [1 1 0]))
