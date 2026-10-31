# Indicações (IND)

> [Índice das histórias](README.md) · Regras gerais: [RG-01 a RG-14](README.md#regras-gerais) · [Modelo de dados](../modelo-de-dados/README.md)

Substitui a aba INDICACOES. Toda indicação tem um associado indicador identificado, porque a planilha já alertava que, sem ele, não há como atribuir a indicação.

### IND-01 · Registrar indicação

**Como** membro da equipe, **quero** registrar quem um cliente indicou, **para** a indicação virar uma oportunidade de venda.

**Regras**

- Obrigatórios: associado indicador (buscado na carteira), nome e telefone do indicado. Opcionais: veículo de interesse e observação.
- É obrigatório marcar que o indicado autorizou o contato. Sem isso a indicação não é gravada ([RG-11](README.md#regras-gerais)).
- Nasce com status Recebida, canal Equipe, data de hoje e o autor logado.
- O telefone segue a normalização da [CAR-04](02-carteira.md#car-04--atualizar-contatos-do-cliente).
- Se o telefone já está numa indicação em aberto, o sistema avisa quem indicou antes **(proposta)**. Se já é de um associado, avisa que a pessoa já é cliente **(proposta)**.

**Critérios de aceite**

- Dado o formulário sem a autorização marcada, quando tento gravar, então o sistema recusa.
- Dado um indicador sem cadastro na carteira, então não é possível registrar.

### IND-02 · Acompanhar e atualizar indicação

**Como** vendedor ou gerencial, **quero** acompanhar e atualizar o andamento das indicações, **para** não deixar oportunidade parada.

**Regras**

- Lista com data, indicador, indicado, telefone, status, dias desde o recebimento e placa fechada. Filtros por status, período e indicador.
- Status: Recebida → Em negociação → Convertida ou Perdida. De Recebida pode ir direto para Convertida ou Perdida.
- Perdida exige o motivo na observação **(proposta)**. Convertida e Perdida são finais; só o gerencial reabre **(proposta)**.
- O vendedor vê todas as indicações, porque são oportunidades e não clientes da carteira dele **(proposta)**. O atendente registra, mas não muda o status.
- O indicado tem botão de WhatsApp ([FIL-05](03-filas-e-regua.md#fil-05--abrir-conversa-no-whatsapp)).
- Toda mudança de status vai para a auditoria.

**Critérios de aceite**

- Dada uma indicação Perdida sem observação, quando tento gravar, então o sistema pede o motivo.
- Dado o atendente, quando abre uma indicação, então não consegue mudar o status.

### IND-03 · Converter indicação em contrato

**Como** vendedor ou gerencial, **quero** ligar a indicação ao contrato fechado, **para** saber quais clientes trazem negócio.

**Regras**

- Marcar como Convertida exige o contrato gerado: escolhe-se um existente ou cadastra-se um novo ([CAR-02](02-carteira.md#car-02--cadastrar-cliente-veículo-e-contrato)), com nome e telefone do indicado já preenchidos.
- Cada contrato pode vir de uma única indicação. O contrato substitui a coluna "placa fechada" da planilha.
- A conversão conta para o indicador em [INT-03](06-inteligencia.md#int-03--ver-indicações-por-associado) e aparece para ele na área do cliente ([CLI-05](08-area-do-cliente.md#cli-05--acompanhar-minhas-indicações)).

**Critérios de aceite**

- Dada uma indicação em negociação, quando marco Convertida sem escolher contrato, então o sistema recusa.
- Dado um contrato já ligado a uma indicação, quando tento ligá-lo a outra, então o sistema recusa.
