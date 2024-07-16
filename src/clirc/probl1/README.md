# Tarefa 2 -- Teoria da Computação -- 2024/1
**Definindo Computação & Açucar Sintático & Código como Dados, Dados como Código**
**Autores:** Mauricio Uhlig & Serenna Ferrari

## Problema 1
O primeiro problema da lista é dividido em 3 partes, cada uma com um objetivo diferente.

A parte I pede a construção de uma função no dialeto AON-CLIRC que compare 2 números binários de 3 bits cada, retornando 1 (verdadeiro) quando o número
da esquerda for maior que o da direita, e 0 em qualquer outro caso.

A parte II pede uma prova que existe uma constante para descobrir o número máximo de portas lógicas para computar a função de maior (mesmas regras 
discutidas na parte I) no dialeto AON-CLIRC.

Por fim, a parte III requere a mesma prova pedida na parte II, porém para portas NAND.

**Solução - Parte I**

A solução para a função pedida foi resolvida com o código para a prova requerida pela parte II. 

Ao desenhar o circuito lógico que produzia a resposta para função GT, pudemos notar o padrão com a constante requerida pela parte II.

Provamos que essa constante existe com um gerador de código CLIRC-AON para a função GT_2n, onde só precisamos dizer qual o N para termos nosso programa em CLIRC pronto para ser "avaliado" pelo código core. O código gerado para GT_6 a ser avaliado pelo core é o seguinte:

```clojure
[
 (set! temp1 (not (:in 5)))
 (set! temp2 (and (:in 2) temp1))
 (set! temp3 (not (:in 4)))
 (set! temp4 (and (:in 1) temp3))
 (set! temp5 (or (:in 1) temp3))
 (set! temp6 (and temp2 temp5)) 
 (set! temp7 (or temp6 temp4))
 (set! temp8 (not (:in 3)))
 (set! temp9 (and (:in 0) temp8))
 (set! temp10 (or (:in 0) temp8))
 (set! temp11 (and temp7 temp10)) 
 (set! temp12 (or temp11 temp9)) 
 (set! (:out 0) (and temp12 temp12))
]
```

**Execução - I**
Para executar a função (para obter o resultado da avaliação) no prompt do Clojure execute, siga este exemplo:
```clojure
#!=> lein repl
(ns clirc.core (:require [clirc.probl1.probl1 :as cmp]))
(eval-prog-aon (cmp/cmp-n-bits-aon 3) [0 0 1 0 0 0])
```

**Solução - Parte II**

Para essa solução, o ponto inicial foi desenhar um circuito booleano e adicionar
as comparações bit a bit para identificar o padrão. Com esse número observado (no caso
do AON, a constante é 5), o código foi criado de forma a checar o caso inicial 
(comparação de números de 1 bit) e a partir ele, repetir a estrutura de adicionar as 5 portas
lógicas necessárias para formar a lista no dialeto AON-CLIRC, a qual é retornada e pode ser
avaliada.

**Execução - II**
Para testar a função que utiliza a constante no prompt do Clojure execute, use como exemplo:
```clojure
#!=> lein repl
(ns clirc.core (:require [clirc.probl1.probl1 :as cmp]))
(eval-prog-aon (cmp/cmp-n-bits-aon 6) [0 0 1 0 0 1 0 0 0 1 0 0])
```


**Solução - Parte III**

Esta solução seguiu as mesmas linhas da Solução II, com a diferença da porta lógica utilizada.
Ao avaliar o circuito necessário para realizar a comparação entre n bits, a quantidade de portas
constante a ser adicionada para cada bit adicional foi 6.

Para a prova, adaptamos o código gerador de CLIRC-AON para gerar CLIRC-NAND, funcionando tal qual
o primeiro

**Execução - III**
Para executar a função usando portas NAND no prompt do Clojure execute, por exemplo:
```clojure
#!=> lein repl
(ns clirc.core (:require [clirc.probl1.probl1 :as cmp]))
(eval-prog-nand (cmp/cmp-n-bits-nand 3) [0 0 1 0 0 0])
```