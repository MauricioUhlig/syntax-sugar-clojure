# Tarefa 2 -- Teoria da Computação -- 2024/1
**Definindo Computação & Açucar Sintático & Código como Dados, Dados como Código**
**Autores:** Mauricio Uhlig & Serenna Ferrari

## Problema 2

O segundo problema lida com Abstract Syntax Tree (AST) para o açúcar sintático da estrutura de controle IF para a linguagem CLIRC.

A parte I requer uma função em Clojure que traduza um programa CLIRC com a estrutura de controle IF (CLIRC-IF) para a versão sem esse açúcar sintático.

Já a parte II pede uma prova que envolve uma função que tem o comportamento de uma estrutura IF. A prova está descrita no documento, pois não foi necessário código.

**Solução - Parte I**

Para produzir o programa $P'$ sem o açúcar sintático, dividimos em etapas o processamento.

Primeiramente produzimos a AST do programa de entrada $P$, identificando as partes da estrutura (condição, then, else),
renomeando as variáveis presentes de acordo com a "parte" que está localizada (ex.: variável `a` dentro do bloco then, vira `a$then1`).

Com as variáveis renomeadas, processamos então a AST de modo a construir a saída com o mesmo comportamento do IF, mas usando portas lógicas (em AON) que
dão o resultado equivalente.

O resultado será a lista de entrada sem a estrutura IF, esta estará representada pelas variáveis que a substituíram e tem o mesmo comportamento.

Exemplo:

```clojure
Lista de entrada:

[(set! a (and (:in 1) (:in 2)))
(if (:in 0)
[(set! a (and (:in 1) (:in 2)))
(set! b (or (:in 1) (:in 2)))]
[(set! a (xor (:in 1) (:in 2)))
(set! b (nand (:in 1) (:in 2)))])]

Lista de saída:

[(set! a (and (:in 1) (:in 2)))
(set! a$then1 (and (:in 1) (:int 2)))
(set! b$then1 (or (:in 1) (:in 2)))
(set! a$else1 (xor (:in 1) (:in 2)))
(set! b$else1 (nand (:in 1) (:in 2)))
(set! a (iff (:in 0) a$then1 a$else1))
(set! b (iff (:in 0) b$then1 b$else1))]

```



**Execução**
Para executar a função no prompt do Clojure execute, por exemplo:
```clojure
#!=>lein repl

(def if-code-example '[(if (:in 0) [(set! a (and (:in 1) (:in 2))) (set! b (or (:in 1) (:in 2)))] [(set! a (xor (:in 1) (:in 2)))(set! b (nand (:in 1) (:in 2)))])])

(iff/expand-if if-code-example)

```