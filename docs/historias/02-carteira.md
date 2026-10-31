# Carteira de clientes (CAR)

> [Índice das histórias](README.md) · Regras gerais: [RG-01 a RG-14](README.md#regras-gerais) · [Modelo de dados](../modelo-de-dados/README.md)

A carteira substitui a aba CARTEIRA. A lista tem uma linha por contrato (placa), como hoje, mas a pessoa é cadastrada uma só vez.

### CAR-01 · Buscar e filtrar a carteira

**Como** membro da equipe, **quero** encontrar clientes por nome, placa, documento ou telefone e filtrar a lista, **para** chegar rápido a quem preciso atender.

**Regras**

- Colunas: cliente e documento ([RG-04](README.md#regras-gerais)), placa e modelo, cidade, mensalidade, situação, etapa pendente, próximo contato com a fila, e vendedor.
- Filtros: fila (Hoje, Atrasado, Futuro, Sem data, Suspenso, Cancelado), cidade, situação, etapa, tipo de veículo, perfil, potencial de indicação e vendedor. O filtro de vendedor não aparece para o vendedor ([RG-03](README.md#regras-gerais)).
- A busca segue a [RG-05](README.md#regras-gerais): "HAA-7216", "haa7216" e "103.034" funcionam.
- O rodapé mostra a quantidade de contratos e a soma das mensalidades do filtro, com o aviso da [RG-09](README.md#regras-gerais).
- Ordenação padrão por nome, alterável em qualquer coluna. Páginas de 10, 25 ou 50 linhas.
- Ao voltar da ficha, a lista mantém filtros, ordenação e página **(proposta)**.

**Critérios de aceite**

- Dado um contrato com placa HAA7216, quando busco "haa-7216", então ele aparece.
- Dados os filtros Atrasado e Formiga, quando aplico, então vejo só contratos de Formiga com próximo contato antes de hoje, e o rodapé soma só esses.

### CAR-02 · Cadastrar cliente, veículo e contrato

**Como** vendedor ou gerencial, **quero** cadastrar um novo contrato, **para** que o cliente entre na régua de pós-venda.

**Regras**

- O cadastro começa pelo CPF/CNPJ. Se a pessoa já existe, os dados dela são carregados e só o novo veículo e contrato são informados.
- Obrigatórios: nome, CPF/CNPJ, WhatsApp, cidade/UF, placa, modelo, tipo de veículo, mensalidade e data de adesão (cadastro no SGA). Opcionais: e-mail, nascimento (só pessoa física), código do SGA e data da venda.
- CPF/CNPJ com dígitos verificadores válidos **(proposta)**. O CNPJ alfanumérico é aceito.
- Placa no padrão antigo ou Mercosul. Se a placa já tem contrato que não está cancelado, o cadastro é recusado com link para a ficha existente.
- A data de adesão não pode ser futura **(proposta)**. Mensalidade maior ou igual a zero.
- Todo contrato nasce "A conferir". O vendedor do contrato é quem cadastra; o gerencial escolhe o vendedor.
- Ao salvar, o contrato entra na régua com etapa D+7 pendente e próximo contato = adesão + 7 dias ([RG-14](README.md#regras-gerais)).
- Pessoa, veículo, contrato e régua são gravados juntos: se algo falha, nada é gravado.

**Critérios de aceite**

- Dada a adesão em 13/03/2026, quando salvo, então o próximo contato é 20/03/2026 e a etapa pendente é D+7.
- Dado um CPF já cadastrado, quando o informo, então o sistema carrega a pessoa e não cria um segundo cadastro.
- Dada uma placa com contrato ativo, quando tento cadastrá-la de novo, então vejo o erro e o link para a ficha.

### CAR-03 · Ver a ficha do cliente

**Como** membro da equipe, **quero** ver tudo sobre o cliente numa tela, **para** atender sabendo o histórico dele.

**Regras**

- Cabeçalho: nome, fila com dias de atraso, situação, perfil, potencial, botão de WhatsApp e botão de editar.
- Régua do contrato: as etapas, com a pendente destacada e a data prevista de cada uma.
- Dados da pessoa, contatos, e os veículos e contratos dela.
- Histórico de atendimentos, do mais recente ao mais antigo, com as correções indicadas ([ATD-04](04-atendimentos.md#atd-04--corrigir-um-atendimento)).
- Indicações feitas por este cliente.
- Ao lado, o formulário de registrar atendimento ([ATD-01](04-atendimentos.md#atd-01--registrar-atendimento-de-pós-venda)).
- O vendedor só abre a ficha de quem tem contrato dele e só vê esses contratos ([RG-03](README.md#regras-gerais)).

**Critérios de aceite**

- Dado o atendente, quando abre uma ficha, então o documento aparece mascarado.
- Dado um cliente com duas placas, quando abro a ficha, então vejo os dois contratos, cada um com sua régua.

### CAR-04 · Atualizar contatos do cliente

**Como** membro da equipe, **quero** manter WhatsApp, telefones e e-mails corretos, **para** conseguir falar com o cliente.

**Regras**

- O cliente pode ter vários contatos de cada tipo, com um principal por tipo. O WhatsApp principal é o usado nas filas.
- O telefone é aceito com ou sem 55, parênteses, espaços e traços. O DDD é obrigatório.
- O mesmo número repetido no mesmo cliente é recusado. Em outro cliente, o sistema avisa mas permite, porque famílias dividem números **(proposta)**.
- "Não aceita contato" (opt-out) mantém o número visível, mas desativa o botão de WhatsApp com o aviso do motivo.
- O cliente precisa ter ao menos um WhatsApp ou telefone principal **(proposta)**.
- O atendente pode editar contatos; é o único dado cadastral que ele altera.

**Critérios de aceite**

- Dado "(37) 99869-2950", quando salvo, então fica gravado como 5537998692950 e exibido como (37) 9 9869-2950.
- Dado um contato com opt-out, quando o cliente aparece na fila, então o botão de WhatsApp fica desativado.

### CAR-05 · Alterar situação, mensalidade e vendedor do contrato

**Como** gerencial, **quero** atualizar o contrato conforme o SGA, **para** que filas e indicadores reflitam a realidade.

**Regras**

- Só o gerencial altera situação, mensalidade, vendedor e código do SGA.
- Situações possíveis: de "A conferir" para Ativo, Suspenso ou Cancelado; entre Ativo e Suspenso; de qualquer uma para Cancelado.
- Cancelado é definitivo: para voltar, cadastra-se um novo contrato **(proposta)**. Cancelar pede confirmação, grava a data de encerramento e tira o contrato das filas. O histórico continua.
- Suspenso sai das filas e dos aniversariantes. Ao reativar, volta com o próximo contato calculado e, se a data já passou, entra em Atrasados.
- Reajuste de mensalidade guarda só o valor atual; o valor anterior fica na auditoria.
- Trocar o vendedor muda na hora quem enxerga o contrato.
- Ação em lote para marcar vários contratos "A conferir" como Ativo, já que os 284 importados chegam assim **(proposta)**.

**Critérios de aceite**

- Dado um contrato cancelado, quando abro a fila de atrasados, então ele não aparece, e na ficha os atendimentos anteriores continuam lá.
- Dado um reajuste de R$ 124,82 para R$ 139,90, quando salvo, então a auditoria mostra os dois valores, quem alterou e quando.

### CAR-06 · Classificar perfil e potencial de indicação

**Como** membro da equipe, **quero** marcar o perfil do cliente e o potencial de indicação, **para** direcionar ações de relacionamento.

**Regras**

- Perfil: Regular (padrão), Promotor ou Atenção. Potencial: Não avaliado (padrão), Alto, Médio ou Baixo.
- Qualquer papel interno pode alterar, o vendedor só nos seus clientes. A alteração vai para a auditoria.
- O cliente nunca vê essas classificações.
- Alimentam os indicadores "Clientes promotores" e "Indicação potencial alto" ([INT-01](06-inteligencia.md#int-01--ver-indicadores-da-carteira)).

**Critérios de aceite**

- Dado um cliente marcado como Promotor, quando abro a Inteligência, então ele entra na contagem de promotores.

### CAR-07 · Exportar lista de clientes

**Como** gerencial ou vendedor, **quero** exportar a lista filtrada, **para** trabalhar fora do sistema quando necessário.

**Regras**

- Exporta a lista exatamente como filtrada na carteira ou numa fila, em CSV ou Excel.
- O vendedor exporta só os seus contratos. O atendente não exporta **(proposta)**.
- O documento sai mascarado, exceto para o gerencial **(proposta)**. Contatos com opt-out saem marcados.
- Cada exportação fica na auditoria com o filtro usado e a quantidade de linhas.

**Critérios de aceite**

- Dado o vendedor, quando exporta a carteira sem filtro, então o arquivo tem só os contratos dele.

### CAR-08 · Anonimizar cliente (LGPD)

**Como** gerencial, **quero** anonimizar os dados de um cliente que pediu exclusão, **para** cumprir a LGPD sem perder os números históricos.

**Regras**

- Só o gerencial anonimiza, e só quando todos os contratos do cliente estão cancelados **(proposta)**.
- O nome vira "Cliente anonimizado nº" seguido do código interno (por exemplo, nº 1042). Documento, nascimento e contatos são apagados, e a conta da área do cliente é desativada.
- Contratos (valores, cidade, datas) e atendimentos continuam, para os relatórios. A descrição dos atendimentos é apagada, porque pode conter dados pessoais **(proposta)**.
- É irreversível: a confirmação exige digitar ANONIMIZAR.
- **Modelo de dados:** `associado.documento` aceita vazio somente quando `anonimizado_em` está preenchido (`ck_associado_documento_obrigatorio`).

**Critérios de aceite**

- Dado um cliente anonimizado, quando busco pelo CPF antigo, então nada aparece, mas os indicadores por cidade continuam contando o contrato dele.
