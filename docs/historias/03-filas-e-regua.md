# Painel, filas e régua (FIL)

> [Índice das histórias](README.md) · Regras gerais: [RG-01 a RG-14](README.md#regras-gerais) · [Modelo de dados](../modelo-de-dados/README.md)

As filas substituem as abas CENTRAL DO DIA, ATRASADOS e ANIVERSARIANTES. Nos dois sistemas, quem entra em cada fila é decidido pela data do próximo contato ([RG-14](README.md#regras-gerais)).

### FIL-01 · Ver o painel

**Como** membro da equipe, **quero** ver ao entrar quanto trabalho tenho hoje, **para** organizar o dia.

**Regras**

- Indicadores: clientes na carteira (contratos não cancelados, por tipo de veículo; a planilha contava todas as linhas) **(proposta)**, contatos de hoje, atrasados, aniversariantes de hoje e dos próximos 7 dias, mensalidades ativas (soma dos contratos Ativo) e atendimentos no mês (mês corrente, todos os tipos).
- Se houver contratos "A conferir", aparece um aviso com a quantidade e o link para filtrá-los na carteira.
- Abaixo: os próximos contatos de hoje (até 5), a situação dos contratos e os próximos aniversariantes.
- Cada indicador abre a fila ou o filtro correspondente.
- O vendedor vê tudo só dos seus contratos ([RG-03](README.md#regras-gerais)).

**Critérios de aceite**

- Dada a carteira importada, quando o gerencial abre o painel em 31/10/2026, então os números conferem com a planilha aberta no mesmo dia.
- Dado o clique em Atrasados, então abre a fila de atrasados.

### FIL-02 · Trabalhar a fila de contatos de hoje

**Como** membro da equipe, **quero** a lista de quem devo contatar hoje, **para** cumprir a régua sem procurar na carteira.

**Regras**

- Entram os contratos com régua ativa, situação A conferir ou Ativo e próximo contato igual a hoje.
- Colunas, como na planilha: cliente, WhatsApp, veículo, placa, mensalidade, data, etapa e cidade, com as ações WhatsApp, Registrar e Ver ficha.
- Ordem por nome **(proposta; a planilha seguia a ordem das linhas)**. Filtros por cidade, etapa e tipo de veículo, e busca ([RG-05](README.md#regras-gerais)).
- Registrar abre o formulário na própria fila. Depois de gravar, a fila é atualizada e o cliente sai dela se o próximo contato mudou.
- Se outra pessoa registrou o mesmo cliente nesse meio-tempo, a gravação é recusada ([RG-06](README.md#regras-gerais)). É o equivalente ao aviso "A fila mudou" da macro.

**Critérios de aceite**

- Dado um cliente na fila de hoje, quando registro Concluído, então ele sai da fila e o contador do menu diminui em 1.
- Dado um contrato suspenso com próximo contato hoje, então ele não aparece na fila.

### FIL-03 · Trabalhar a fila de atrasados

**Como** membro da equipe, **quero** ver quem ficou sem contato no prazo, **para** recuperar o relacionamento.

**Regras**

- Entram os contratos com régua ativa, situação A conferir ou Ativo e próximo contato antes de hoje.
- Mostra os dias de atraso. A ordem padrão é do mais atrasado para o mais recente.
- Colunas, ações, filtros e atualização iguais aos da [FIL-02](#fil-02--trabalhar-a-fila-de-contatos-de-hoje).

**Critérios de aceite**

- Dada a carteira importada sem nenhum contato registrado, então quase todos os contratos aparecem como atrasados, com o próximo contato em adesão + 7 dias, como na planilha.
- Dado um atrasado em que registro Sem resposta, então ele sai dos atrasados e volta daqui a 2 dias na fila de hoje.

### FIL-04 · Ver aniversariantes

**Como** membro da equipe, **quero** saber quem faz aniversário, **para** mandar uma mensagem no dia.

**Regras**

- Uma linha por pessoa, não por placa. Entram as pessoas com nascimento no dia e mês de hoje e pelo menos um contrato que não esteja suspenso nem cancelado.
- Quem nasceu em 29/02 aparece em 28/02 nos anos não bissextos.
- Abas: Hoje, Próximos 7 dias e Próximos 30 dias **(proposta; a planilha mostrava só hoje)**.
- Colunas: cliente, WhatsApp, veículo e placa, mensalidade, dia do aniversário, situação e cidade.
- A ação principal é registrar a mensagem de aniversário ([ATD-03](04-atendimentos.md#atd-03--registrar-mensagem-de-aniversário)). Depois de registrada, a linha mostra "Mensagem enviada" **(proposta)**.
- Pessoa jurídica não tem nascimento e nunca aparece.

**Critérios de aceite**

- Dado um cliente nascido em 29/02/1992, quando a data é 28/02/2027, então ele aparece em Hoje.
- Dado um cliente com duas placas que faz aniversário hoje, então ele aparece uma única vez.

### FIL-05 · Abrir conversa no WhatsApp

**Como** membro da equipe, **quero** abrir a conversa com um clique, **para** não digitar o número.

**Regras**

- O botão abre wa.me com o WhatsApp principal do cliente (55 + DDD + número), numa nova aba. Nada é enviado sozinho ([RG-10](README.md#regras-gerais)).
- Sem WhatsApp, o botão some e aparece "Sem WhatsApp". Com opt-out, o botão fica desativado ([CAR-04](02-carteira.md#car-04--atualizar-contatos-do-cliente)).
- Abrir a conversa não registra atendimento: o registro é um passo separado.
- Texto inicial sugerido por tipo de contato (pós-venda, aniversário), editável antes de enviar **(proposta)**.

**Critérios de aceite**

- Dado o WhatsApp 5537998692950, quando clico, então abre https://wa.me/5537998692950.

### FIL-06 · Configurar prazos da régua

**Como** gerencial, **quero** ajustar os dias entre os contatos da régua, **para** adaptar o pós-venda sem depender de programador.

**Regras**

- Só o gerencial altera. Cada etapa aceita de 1 a 365 dias. A sequência das etapas é fixa nesta tela; criar etapa nova é uma mudança de sistema **(proposta)**.
- Ao salvar, o próximo contato de todos os contratos com régua ativa é recalculado. Os que têm retorno manual mantêm a data manual.
- Antes de confirmar, o sistema mostra quantos contratos vão mudar de data **(proposta)**.
- A alteração vai para a auditoria ([RG-08](README.md#regras-gerais)).

**Critérios de aceite**

- Dado o D+7 concluído em 25/03/2026, quando mudo o prazo do D+30 de 23 para 25 dias, então o próximo contato passa de 17/04 para 19/04/2026.
- Dado um contrato com retorno manual em 05/11/2026, quando mudo qualquer prazo, então a data continua 05/11/2026.
