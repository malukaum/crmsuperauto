# Importação da planilha (IMP)

> [Índice das histórias](README.md) · Regras gerais: [RG-01 a RG-14](README.md#regras-gerais) · [Modelo de dados](../modelo-de-dados/README.md)

A importação leva os dados atuais para o sistema e substitui a aba BASE HISTORICA como registro de auditoria. Serve para a carga inicial e para cargas complementares durante o uso em paralelo.

### IMP-01 · Importar a planilha

**Como** gerencial, **quero** importar a planilha atual, **para** começar a usar o sistema sem redigitar nada.

**Regras**

- Só o gerencial importa. Aceita .xlsx e .xlsm de até 10 MB **(proposta)**. Macros do arquivo são ignoradas e nunca executadas.
- O mesmo arquivo (mesmo conteúdo) não pode ser importado duas vezes.
- As abas e colunas são achadas pelo nome do cabeçalho, não pela posição, então uma coluna movida não quebra a importação.
- **CARTEIRA:** cada linha vira associado (agrupado pelo CPF/CNPJ), contato, veículo, contrato (origem Importação) e régua, conforme o mapeamento do modelo de dados.
- A cidade é casada com o IBGE pelo nome e UF, sem diferença de acento. Sem correspondência, a linha entra sem cidade e com aviso **(proposta)**.
- Placa que já tem contrato não cancelado no sistema: a linha fica como Duplicado e nada é sobrescrito.
- Etapa "Não iniciado" vira D+7 pendente e "Concluído" vira régua encerrada. Se Último contato e Etapa concluída estiverem preenchidos, é criado um atendimento Concluído com essa data.
- **ATENDIMENTOS:** cada linha é ligada ao contrato pela placa. Com a placa vazia, pelo nome exato do cliente **(proposta)**; sem correspondência, a linha é rejeitada. Grafias sem acento ("Pos-venda", "Concluido") são reconhecidas. O autor é o usuário técnico "Importação da planilha"; os logins do Windows são ignorados.
- **INDICACOES:** o indicador é achado pelo nome na carteira; sem correspondência, a linha é rejeitada.
- **CONFIGURACOES:** os prazos só são importados se o gerencial marcar "importar prazos" **(proposta)**.
- **BASE HISTORICA:** cada linha é guardada como veio. Das placas repetidas, só a mais recente vira contrato; as anteriores ficam como Duplicado.
- Uma linha com erro não interrompe as outras. Um arquivo ilegível ou sem a aba CARTEIRA cancela tudo, e a importação fica como Falhou.
- No fim, o próximo contato de todos os contratos importados é calculado ([RG-14](README.md#regras-gerais)).

**Critérios de aceite**

- Dada a planilha atual, quando importo, então o sistema tem 284 contratos e guarda as 286 linhas da base histórica, 2 delas como Duplicado.
- Dado o mesmo arquivo importado de novo, então o sistema recusa antes de ler.

### IMP-02 · Consultar resultado da importação e base histórica

**Como** gerencial, **quero** ver o que entrou e o que ficou de fora, **para** corrigir a planilha e confiar nos números.

**Regras**

- Lista de importações: arquivo, data, quem importou, status e totais de linhas importadas, duplicadas e rejeitadas.
- O detalhe mostra cada linha: aba, número da linha na planilha, situação, mensagem e dados originais.
- As linhas rejeitadas podem ser baixadas numa planilha para correção **(proposta)**.
- A base histórica (as linhas originais, como vieram) pode ser consultada com busca por placa e nome.

**Critérios de aceite**

- Dada uma linha rejeitada, então o detalhe mostra o número dela na planilha e o motivo, por exemplo "Placa vazia e cliente não encontrado".
