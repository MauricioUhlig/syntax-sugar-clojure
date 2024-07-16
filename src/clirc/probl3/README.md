# Tarefa 2 -- Teoria da Computação -- 2024/1
**Definindo Computação & Açucar Sintático & Código como Dados, Dados como Código**
**Autores:** Mauricio Uhlig & Serenna Ferrari

## Problema 3

O problema 3 é dividido em três partes, bem diretas. A primeira pede a criação do açúcar sintático para FOR, criando a função de multiplicação de 2 números de 3 bits. 

Com a parte I resolvida, a parte II pede para generalizar a função de multiplicação, passando a aceitar N bits e gerando o CÓDIGO CLIRC para a multiplicação.

A parte III pede a prova que o programa gerado na parte II não ultrapassará $1000 * n²$ linhas de código; a prova está descrita no documento, não sendo abordada no README.md

**Solução - Parte I**

Para criar CLIRC-FOR e usá-lo na função de multiplicação, optamos por também criar um açúcar sintático "PROC", este é responsável por abstrair operações mais complexas e permitir a reutilização (por exemplo, criação do XOR usando AON, o qual é usado para a multiplicação).

A lógica consiste em fixar o multiplicador e fixar o multiplicando, gerando assim resultados intermediários da multiplicação de cada bit do multiplicador pelo multiplicando. A cada operação realizada, o resultado parcial (a soma de bits, utilizando o carry quando necessário) era calculado, formando assim o resultado final.


**Execução**
Para executar a multiplicação de $111 * 111$ (7*7), execute no prompt:
```clojure
#!=>lein repl
(ns clirc.core (:require [clirc.probl3.probl3 :as f4r]))
(eval-prog-aon f4r/proc-code-3 [1 1 1 , 1 1 1])
```
A saída esperada é $110001$ (49).


**Solução - Parte II**

Ao desenvolver a solução da I, pudemos notar padrões interessantes que nos ajudaram a criar a função pedida pela parte II.

- A saída (out) terá $2n$ bits, variando de $0$ à $2n − 1$
- O FOR que irá realizar a multiplicação dever ́a realizar $n$ iterações, com o iterador $i$ variando de $0$ à $n − 1$
- Existem $n$ blocos de mult, que são responsáveis por calcular a multiplicação e carry

Sabendo dessas características, criamos a função em Clojure que monta a função CLIRC-FOR para a multiplicação de $N$ bits.

**Execução**
Para obter o código CLIRC, execute:
```clojure
#!=>lein repl
;;Para visualizar o programa em CLIRC para multiplicação de números de 4 bits, execute:
(println (f4r/mult-N-bits-clirc 4))

;;Para confirmar que a função gerada funciona como deveria, execute:
(eval-prog-aon (f4r/mult-N-bits-clirc 4) [0 0 0 1 1 0 0 0])

;;Resultado esperado: [0 0 0 0 1 0 0 0]
```
