# TS-Framework: Solucionador de Busca Tabu para o Problema Máximo de Cobertura de Vértices em Grafos Bipartidos Quadráticos (Max-SCQBF)

## Visão Geral

Este projeto, implementa uma meta-heurística de Busca Tabu (Tabu Search - TS) para resolver o Problema Máximo de Cobertura de Vértices em Grafos Bipartidos Quadráticos (Max-SCQBF). O framework é projetado para ser flexível, permitindo a experimentação com diferentes configurações da Busca Tabu para encontrar soluções de alta qualidade.

A implementação é centrada na classe `TS_MAXSCQBF`, que orquestra a execução do algoritmo de Busca Tabu, e utiliza uma estrutura modular que separa a lógica da meta-heurística (`AbstractTS`), a definição do problema (`MaxSCQBF`) e a representação da solução (`Solution`).

## Funcionalidades

* **Implementação da Busca Tabu:** Um algoritmo de Busca Tabu abstrato e genérico que pode ser estendido para outros problemas de otimização.
* **Resolvedor Específico para Max-SCQBF:** Implementação detalhada dos movimentos de vizinhança (inserção, remoção e troca) e estratégias de busca (primeira melhora e melhor melhora) para o problema Max-SCQBF.
* **Configurações de Execução Flexíveis:** Permite a configuração de parâmetros chave via linha de comando, como diretório de instâncias, arquivo de saída, tempo limite, número de iterações e tamanho da lista tabu (tenure).
* **Múltiplas Estratégias de Busca:** Suporta diferentes modos de busca, incluindo "first-improving" e "best-improving", além de uma variação probabilística.
* **Geração de Relatórios:** Salva os resultados dos experimentos em um arquivo CSV para fácil análise e comparação de desempenho.

## Estrutura do Projeto

O código-fonte está localizado dentro da pasta `TS-Framework/src` e está organizado nos seguintes pacotes principais:

* `metaheuristics.tabusearch`: Contém a classe abstrata `AbstractTS`, que define a estrutura geral do algoritmo de Busca Tabu.
* `problems.qbf`: Inclui a classe `MaxSCQBF`, que modela o problema Max-SCQBF, e outras classes relacionadas à leitura e avaliação de instâncias do problema.
* `problems.qbf.solvers`: Contém a classe principal `TS_MAXSCQBF`, que especializa a Busca Tabu para o problema Max-SCQBF, e suas variações.
* `solutions`: Apresenta a classe `Solution`, que representa uma solução para o problema de otimização.

## Pré-requisitos

* **Java 21 ou superior:** O código foi desenvolvido e testado utilizando Java 21. Certifique-se de ter o JDK (Java Development Kit) 21 ou uma versão mais recente instalada.

## Compilação e Execução

A execução do projeto é centralizada na classe `TS_MAXSCQBF.java`. Para compilar e executar o projeto a partir da pasta raiz do seu projeto, siga os passos abaixo:

1.  **A partir da raiz do seu projeto**, compile os arquivos-fonte Java:

    ```bash
    javac -d bin TS-Framework/src/metaheuristics/tabusearch/*.java TS-Framework/src/problems/*.java TS-Framework/src/problems/qbf/*.java TS-Framework/src/problems/qbf/solvers/*.java TS-Framework/src/solutions/*.java
    ```

2.  **Execute a classe principal `TS_MAXSCQBF`:**

    A execução é feita através da linha de comando, passando os argumentos necessários.

    ```bash
    java -cp bin problems.qbf.solvers.TS_MAXSCQBF [diretorio_instancias] [arquivo_saida_csv] [limite_tempo_segundos] [max_iteracoes] [tenure_1] [tenure_2]
    ```

### Argumentos de Linha de Comando

A classe `TS_MAXSCQBF` aceita os seguintes argumentos de linha de comando (todos são opcionais e possuem valores padrão):

* **`diretorio_instancias`**: O caminho para o diretório que contém os arquivos de instância do problema (formato `.txt`).
    * **Padrão:** `"TS-Framework/instances/max-sc-qbf2"`
* **`arquivo_saida_csv`**: O nome do arquivo CSV onde os resultados serão salvos.
    * **Padrão:** `"TS-Framework/results/ts_results.csv"`
* **`limite_tempo_segundos`**: O tempo máximo de execução em segundos para cada instância.
    * **Padrão:** `1800`
* **`max_iteracoes`**: O número máximo de iterações que o algoritmo de Busca Tabu executará.
    * **Padrão:** `10000`
* **`tenure_1` (T1)**: O valor do primeiro parâmetro de posse (tenure) da lista tabu.
    * **Padrão:** `20`
* **`tenure_2` (T2)**: O valor do segundo parâmetro de posse (tenure), geralmente o dobro de T1.
    * **Padrão:** `40` (calculado como `2 * T1`)

### Exemplo de Execução

O comando a seguir executa o solucionador com as configurações padrão a partir da raiz do projeto:

```bash
java -cp bin problems.qbf.solvers.TS_MAXSCQBF
```
Para executar com configurações personalizadas (por exemplo, um diretório de instâncias diferente e um tempo limite menor):

```Bash

java -cp bin problems.qbf.solvers.TS_MAXSCQBF "TS-Framework/meu_diretorio/instancias" "TS-Framework/meus_resultados.csv" 600
```
#### Configurações Internas


Dentro do método main da classe TS_MAXSCQBF, existe um array de configurações (configs) que define diferentes estratégias de Busca Tabu a serem executadas para cada instância. As configurações padrão são:

---
PADRAO: Utiliza a estratégia de busca "first-improving" com o tenure padrão (T1).

PADRAO+BEST: Utiliza a estratégia de busca "best-improving" com o tenure padrão (T1).

PADRAO+TENURE: Utiliza a estratégia de busca "first-improving" com um tenure maior (T2).

PROBABILISTIC(SR=0.2): Uma variação probabilística da busca "first-improving" com uma taxa de amostragem de 0.2.

PROBABILISTIC(SR=0.6): Uma variação probabilística da busca "first-improving" com uma taxa de amostragem de 0.6.

Essas configurações permitem comparar facilmente o desempenho de diferentes abordagens da Busca Tabu no mesmo conjunto de instâncias.

---

### Formato do Arquivo de Saída (CSV)

O arquivo de saída CSV gerado conterá as seguintes colunas, permitindo uma análise detalhada dos resultados:

- config: O nome da configuração utilizada (ex: "PADRAO", "PADRAO+BEST").
- file: O nome do arquivo da instância.
- n: O número de variáveis na instância.
- k: Parâmetro 'k' da instância.
- tenure: O valor da posse (tenure) utilizado.
- search_mode: O modo de busca (FIRST_IMPROVING, BEST_IMPROVING).
- time_limit_s: O limite de tempo em segundos.
- max_iterations: O número máximo de iterações.
- timed_out: "true" se a execução atingiu o tempo limite, "false" caso contrário.
- max_value: O valor da função objetivo da melhor solução encontrada (custo invertido).
- size: O número de elementos na melhor solução.
- feasible: "true" se a solução for viável, "false" caso contrário.
- time_s: O tempo total de execução em segundos.
- elements: Os elementos que compõem a melhor solução.

#### Assuntos Relevantes

##### Modos de Busca (SearchMode)
A enumeração SearchMode define as duas principais estratégias de exploração da vizinhança:

- FIRST_IMPROVING: O primeiro movimento de melhora encontrado na vizinhança é aplicado. Essa abordagem é geralmente mais rápida.

- BEST_IMPROVING: Todos os movimentos na vizinhança são avaliados, e o que resulta na maior melhora é aplicado. Essa abordagem é mais completa, mas computacionalmente mais intensiva.

##### Lógica de Movimentos de Vizinhança

Os métodos neighborhoodMoveFirst() e neighborhoodMoveBest() implementam a lógica para explorar a vizinhança da solução atual. Os movimentos considerados são:

- Inserção: Adicionar um elemento que não está na solução.

- Remoção: Remover um elemento da solução.

- Troca (Swap): Trocar um elemento da solução por um que não está.

##### Lista Tabu (TL)

A lista tabu (TL) é implementada como uma ArrayDeque e armazena os movimentos recentes que são temporariamente "proibidos" para evitar ciclos e permitir que a busca escape de ótimos locais. O tamanho da lista é determinado pelo parâmetro tenure.

##### Critério de Aspiração
Um critério de aspiração está implementado para permitir que um movimento tabu seja realizado se ele levar a uma solução melhor do que a melhor solução encontrada até o momento (bestSol). Isso adiciona flexibilidade à busca, permitindo movimentos promissores mesmo que sejam tabu.