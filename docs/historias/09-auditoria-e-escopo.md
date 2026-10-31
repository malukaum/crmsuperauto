# Auditoria (AUD) e fora do escopo

> [Índice das histórias](README.md) · Regras gerais: [RG-01 a RG-14](README.md#regras-gerais) · [Modelo de dados](../modelo-de-dados/README.md)

### AUD-01 · Consultar a trilha de auditoria

**Como** gerencial, **quero** ver quem alterou, viu ou exportou dados, **para** responder a dúvidas e cumprir a LGPD.

**Regras**

- Só o gerencial consulta. Filtros: tipo de registro (cliente, contrato, indicação…), registro específico, usuário, período e operação.
- Operações: criação, alteração, exclusão, visualização de documento, exportação e anonimização.
- A alteração mostra, campo a campo, o valor anterior e o novo.
- A trilha é só de leitura: ninguém altera ou apaga um evento. Retenção de 5 anos **(proposta)**.
- A ficha do cliente tem um atalho para a trilha daquele cliente.

**Critérios de aceite**

- Dado um reajuste de mensalidade, quando abro a trilha do contrato, então vejo o valor anterior, o novo, quem alterou e quando.

### Fora do escopo

Não estão nestas histórias e ficam para fases futuras:

- Boletos, pagamentos, inadimplência e sinistros (dependem de integração com o SGA).
- Sincronização automática de contratos com o SGA.
- Envio automático de mensagens e campanhas em massa por WhatsApp.
- Várias regionais na mesma base.
- Aplicativo nativo: a área do cliente é web, para celular.
