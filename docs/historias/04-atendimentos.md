# Atendimentos (ATD)

> [Índice das histórias](README.md) · Regras gerais: [RG-01 a RG-14](README.md#regras-gerais) · [Modelo de dados](../modelo-de-dados/README.md)

O registro de atendimento substitui a macro `RegistrarAtendimento` e a aba ATENDIMENTOS. É a única forma de mover a régua.

### ATD-01 · Registrar atendimento de pós-venda

**Como** membro da equipe, **quero** registrar o resultado de um contato de pós-venda, **para** que o histórico fique guardado e o próximo contato seja agendado sozinho.

**Regras**

- Pode ser feito pela ficha ou direto pelas filas. Vindo da fila, o contrato já vem escolhido; pela ficha, quem tem mais de uma placa escolhe o contrato.
- Resultados: Concluído, Sem resposta, Reagendado e Pendente.
- Antes de gravar, o sistema mostra o que vai acontecer: etapa concluída, próxima etapa e data do próximo contato. É a confirmação que a macro pedia.
- **Concluído:** a etapa pendente fica registrada como concluída e a régua avança (D+7 → D+30 → D+60 → D+90 → Trimestral → Trimestral). O retorno manual anterior é apagado, e o próximo contato passa a ser a data do atendimento + o prazo da nova etapa.
- **Sem resposta:** a etapa não muda. O próximo contato é a data informada ou, sem data, hoje + 2 dias.
- **Reagendado:** a etapa não muda, e a data combinada com o cliente é obrigatória.
- **Pendente:** etapa e data não mudam; fica só o registro **(proposta: a macro não usava este resultado)**.
- Uma data manual informada em qualquer resultado vale como retorno manual e prevalece sobre o cálculo. Ela não pode ser anterior a hoje.
- Resumo opcional, até 2.000 caracteres.
- Contrato cancelado: recusa com "Contrato cancelado. O atendimento não será gravado." Contrato suspenso pode ser registrado, como na macro.
- O autor é quem está logado. A data e hora é a de agora, ou uma anterior informada pelo usuário, de até 7 dias atrás **(proposta)**.
- Histórico e régua são gravados juntos: se um falha, nenhum é gravado.

**Critérios de aceite**

- Dado um contrato com D+7 pendente, quando registro Concluído em 25/03/2026, então a etapa pendente vira D+30 e o próximo contato vira 17/04/2026.
- Dado um contrato em Trimestral, quando registro Concluído em 01/06/2026, então a etapa continua Trimestral e o próximo contato é 30/08/2026.
- Dado Sem resposta em 31/10/2026, sem data informada, então o próximo contato é 02/11/2026.
- Dado Reagendado sem data, quando tento gravar, então o sistema pede a data.
- Dada uma data manual de ontem, quando tento gravar, então o sistema recusa.

### ATD-02 · Registrar atendimento avulso

**Como** membro da equipe, **quero** registrar contatos que não são do pós-venda, **para** o histórico do cliente ficar completo.

**Regras**

- Tipos: Suporte, Comercial, Reaproximação e Indicação.
- Contato avulso nunca mexe na régua, qualquer que seja o resultado **(proposta: a macro gravava o avulso como pós-venda)**.
- O contrato é opcional: o contato pode ser sobre a pessoa.
- Pode ser registrado para contrato cancelado, o que serve para a reaproximação de ex-clientes **(proposta)**.
- Reagendado continua exigindo data, que fica só como informação no histórico.

**Critérios de aceite**

- Dado um contrato com D+30 pendente para 17/04, quando registro Suporte Concluído, então etapa e data continuam iguais.

### ATD-03 · Registrar mensagem de aniversário

**Como** membro da equipe, **quero** registrar que enviei a mensagem de aniversário, **para** não repetir e para medir o relacionamento.

**Regras**

- Grava o tipo Aniversário, resultado Concluído, ligado à pessoa e não a um contrato.
- Não altera a régua de nenhum contrato, como na macro ("agenda de pós-venda preservada").
- Se já houver mensagem registrada para a pessoa no ano, o sistema avisa antes de gravar outra **(proposta)**.

**Critérios de aceite**

- Dado um aniversariante com D+30 pendente, quando registro a mensagem, então a data do D+30 não muda e a linha passa a mostrar "Mensagem enviada".

### ATD-04 · Corrigir um atendimento

**Como** autor de um registro ou gerencial, **quero** corrigir um atendimento lançado errado, **para** o histórico ficar certo sem perder o rastro.

**Regras**

- Atendimento não é editado nem apagado ([RG-07](README.md#regras-gerais)). A correção é um novo registro que aponta para o original, que passa a aparecer como "corrigido" no histórico.
- O autor pode corrigir até o fim do dia seguinte ao registro; o gerencial, a qualquer momento **(proposta)**.
- A correção não desfaz a régua sozinha. Se o erro mudou a régua (um Concluído por engano, por exemplo), o gerencial ajusta a etapa pendente e o retorno manual na ficha, com registro na auditoria **(proposta)**.

**Critérios de aceite**

- Dado um atendimento corrigido, quando abro o histórico, então vejo os dois registros, o original marcado como corrigido.

### ATD-05 · Consultar o histórico de atendimentos

**Como** membro da equipe, **quero** consultar todos os atendimentos, **para** acompanhar o trabalho e recuperar conversas.

**Regras**

- Lista do mais recente para o mais antigo. Filtros: período (padrão: mês corrente), tipo, resultado, usuário e cidade; busca por cliente ou placa.
- Colunas, como na planilha: data e hora, placa, cliente, tipo, etapa, resultado, descrição, próxima ação, data de retorno e usuário.
- A próxima ação é calculada como na macro: Concluído → "Retorno automático"; Sem resposta e Reagendado → "Nova tentativa"; Aniversário → "Sem alteração no retorno".
- O vendedor vê os atendimentos dos seus contratos e as mensagens de aniversário dos seus clientes.
- Atendimentos vindos da planilha mostram o autor "Importação da planilha".
- A exportação segue as regras da [CAR-07](02-carteira.md#car-07--exportar-lista-de-clientes).

**Critérios de aceite**

- Dado o atendimento importado de 21/10/2026 ("TESTE"), quando abro o histórico de outubro, então ele aparece com o autor "Importação da planilha".
