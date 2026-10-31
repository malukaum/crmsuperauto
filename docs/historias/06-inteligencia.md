# Inteligência comercial (INT)

> [Índice das histórias](README.md) · Regras gerais: [RG-01 a RG-14](README.md#regras-gerais) · [Modelo de dados](../modelo-de-dados/README.md)

Substitui a aba INTELIGENCIA, com os mesmos indicadores e avisos. Todo número é da situação atual da carteira; séries ao longo do tempo ficam para depois do MVP **(proposta)**.

### INT-01 · Ver indicadores da carteira

**Como** gerencial ou vendedor, **quero** ver os números da carteira, **para** saber o tamanho e a qualidade do que estamos acompanhando.

**Regras**

- Indicadores da planilha: registros (contratos), ativos confirmados, a conferir, cancelados, suspensos (novo), soma de todas as mensalidades, mensalidade contratada ativa (soma dos Ativos), recorrência estimada (5% da mensalidade ativa), ticket médio ativo (mensalidade ativa ÷ ativos, zero se não houver ativos), clientes promotores e clientes com potencial de indicação alto.
- A "recorrência estimada 5%" foi mantida como na planilha, mas o que esses 5% representam precisa ser confirmado antes de entrar no sistema **(a validar)**.
- Avisos fixos: "Receita de contratos ativos não é receita recebida" e "Para medir recorrência real, é preciso conciliar pagamentos e inadimplência com o SGA" ([RG-09](README.md#regras-gerais)).
- O gerencial filtra por vendedor; o vendedor vê só os seus números ([RG-03](README.md#regras-gerais)).
- Atendimentos do período, por resultado e por usuário **(proposta)**.

**Critérios de aceite**

- Dada a carteira importada, então os indicadores mostram 284 registros, 284 a conferir, 0 ativos e R$ 102.119,63 no total de mensalidades, iguais aos da planilha.

### INT-02 · Ver desempenho por cidade e tipo de veículo

**Como** gerencial ou vendedor, **quero** ver a carteira por cidade e por tipo de veículo, **para** saber onde concentrar esforço.

**Regras**

- Por cidade, como na planilha: cadastros, soma das mensalidades, ticket médio, ativos e cancelados, do maior número de cadastros para o menor.
- Por tipo de veículo: quantidade de contratos e soma das mensalidades.
- Clicar numa cidade abre a carteira filtrada por ela.
- Mesmos avisos e mesma visibilidade da [INT-01](#int-01--ver-indicadores-da-carteira).

**Critérios de aceite**

- Dada a carteira importada, então Formiga aparece primeiro, com 126 cadastros e R$ 43.214,09.

### INT-03 · Ver indicações por associado

**Como** gerencial ou vendedor, **quero** ver quais clientes mais indicam e quantas indicações viram contrato, **para** reconhecer e estimular os promotores.

**Regras**

- Colunas da planilha: associado indicador, indicações e convertidas, mais a taxa de conversão **(proposta)**.
- Ordenado pelo número de indicações, com filtro de período.
- Sem indicações registradas, a tela explica de onde elas vêm (equipe e área do cliente) em vez de mostrar uma tabela vazia.

**Critérios de aceite**

- Dado um associado com 3 indicações, 1 convertida, então a linha mostra 3, 1 e 33%.
