# Tarefa 2 -- Teoria da Computação -- 2021/2
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
A função criada para a parte II verifica se o número binário da esquerda é maior que o binário da direita para N bits,
como consequência, o mesmo código consegue verificar para números de 3 bits. Mais detalhes da implementação logo abaixo.


**Solução - Parte II**

Para essa solução, o ponto inicial foi desenhar um circuito booleano e adicionar
as comparações bit a bit para identificar o padrão. Com esse número observado (no caso
do AON, a constante é 5), o código foi criado de forma a checar o caso inicial 
(comparação de números de 1 bit) e a partir ele, repetir a estrutura de adicionar 5 portas
lógicas para formar uma lista no dialeto AON-CLIRC, a qual é retornada.



 --- TO DO ---
**Solução - Parte III**

A solução adotada foi implementar uma recursão. O caso base é quando o
comprimento é zero. A única cadeia de tamanho zero é a cadeia vazia. Nos demais
casos, gera-se todas as cadeias de tamanho $n-1$ e em seguida gera-se duas
semi-duplicadas, uma com 0 acrescentando à frente de cada cadeia, e outra com 1
acrescentado à frente de cada cadeia. O resultado é a concatenação das duas
semi-duplicatas.

**Execução**
Para executar a função no prompt do Clojure execute, por exemplo:
```
#> lein repl
(ns clirc.core (:require [clirc.probl1.probl1 :as cmp]))
(def n 3) ;; 3 é o numero de bits que serão comparados
(cmp/cmp-n-bits n) 
(println (aon-eval (cmp/cmp-n-bits n) [0 0 1, 0 0 0])))
```