# Tarefa 2 -- Teoria da Computação -- 2024/1
**Definindo Computação & Açucar Sintático & Código como Dados, Dados como Código**
**Autores:** Mauricio Uhlig & Serenna Ferrari

## Problema 4

A questão 4 trata de tuplas na linguagem CLIRC, dando a definição do livro texto. Há duas partes para a questão:

A parte I requer uma função em Clojure que, obedecendo a definição de lista de tuplas, receba um dicionário de atribuições e retorne a representação
desse dicionário em lista de tuplas.

A parte II também pede uma função, dessa vez recebendo uma lista de tuplas, um programa de comandos em CLIRC e uma sequência binária que representa a entrada do programa.
A função deve retornar os resultados produzidos por esse programa CLIRC em uma sequência binária.

**Solução - Parte I**

Para transformar o dicionário de entrada em uma lista de tuplas, primeiramente lemos todas as variáveis passadas pelo dicionário, usando um parser para transformar o Clirc em código Clojure com a divisão de :input, :output e :workspace. Após a transformação, concatenamos todas as variáveis na respectiva ordem, tendo assim um vetor único.

Com esse vetor pronto, lemos de novo o programa em Clirc provido, procurando os índices das variáveis. Retornamos os índices encontrados na seguinte forma:
```
De: (set! temp (nand a b)) 

Para: [indice(temp) indice(a) indice(b)]

```
Enfim, criamos uma lista com a contagem das variáveis tipo $:input$ e $:output$, seguida da lista de tuplas criadas anteriormente.


**Execução**
Para executar a função no prompt do Clojure execute, por exemplo:
```
lein repl

(def code_clicr '[(set! temp0 (nand (:in 0) (:in 1)))
(set! temp1 (nand temp0 temp0))
(set! temp2 (nand (:in 2) temp1))
(set! (:out 0) (nand temp2 temp1))
(set! (:out 1) (nand temp0 temp1))
])


(println (tuples/clirc2tuples code_clicr))


O resultado esperado da execução ́e:

[3 2 [[3 0 1] [4 3 3] [5 2 4] [6 5 4] [7 3 4]]]
```

**Solução - Parte II**

Para criar a função corretamente, validamos a quantidade de entradas na primeira posição da tupla corresponde ao tamanho da entrada fornecida para a execução. Caso sejam diferentes, não será possível criar a saída pedida.

Caso essa parte esteja correta, criamos um dicionário uma keyword para cada variável existente nas tuplas fornecidas. Para saber quais e quantas variáveis existem, procuramos o maior valor entre todas as tuplas, pois os números das tuplas foram gerados de forma sequencial, com incremento unitário, começando em 0. Cada keyword no dicionário criado inicia-se com o valor $0$ por convenção.

Com o dicionário criado, atribuímos a sequência de entrada de $P$. Para uma entrada de $n$ bits, assumimos que o bit mais à direita é o bit inicial (posição 0) e o mais à esquerda é o bit na posição $n - 1$. Associamos então o bit na posição $0$ à _keyword_ $0$, seguindo assim sucessivamente até o bit na posição $n - 1$.

O próximo passo é relativamente simples: pegamos de cada tupla o segundo e terceiro número, recuperamos seus respectivos valores no dicionário, e aplicamos a operação NAND correspondente, associando o resultado à keyword do dicionário que representa o primeiro número da tupla.

Finalmente, calculamos o _offset_ para determinar a partir de qual _keyword_ encontram-se os _outputs_. Em seguida, retornamos os valores referentes a essas _keywords_.

**Execução**
Para testar o programa, execute:
```
lein repl

(def tuples '[3 2 [[3 0 1] [4 3 3] [5 2 4] [6 5 4] [7 3 4]]])

(println (tuples/run-nand-tuples tuples [1 1 0]))

O resultado esperado é:

[1 1]
```

```