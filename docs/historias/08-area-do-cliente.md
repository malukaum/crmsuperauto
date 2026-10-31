# Área do cliente (CLI)

> [Índice das histórias](README.md) · Regras gerais: [RG-01 a RG-14](README.md#regras-gerais) · [Modelo de dados](../modelo-de-dados/README.md)

Área web pensada para o celular. O cliente vê só os próprios dados, e nenhuma anotação interna da equipe.

### CLI-01 · Fazer o primeiro acesso

**Como** associado, **quero** criar meu acesso com segurança, **para** consultar meus dados sem ligar para a equipe.

**Regras**

- O cliente informa o CPF/CNPJ. Se houver contrato não cancelado e contato principal, o sistema mostra o WhatsApp ou e-mail mascarado (por exemplo, (37) \*\*\*\*\*-2950) e envia um código para o escolhido.
- A resposta na tela é a mesma para documento cadastrado ou não ("Se houver cadastro, enviaremos um código"), para não revelar quem é cliente **(proposta)**.
- Código de 6 dígitos, válido por 15 minutos e com até 5 tentativas **(proposta)**. Fica guardado só em hash.
- Com o código certo, o cliente define a senha. O sistema cria a conta no Keycloak com o papel cliente, login pelo CPF/CNPJ **(proposta)**, e liga a conta ao cadastro.
- Quem já tem conta é levado ao login, com a opção "Esqueci minha senha".
- **Dependência:** mandar o código por WhatsApp exige um provedor de mensagens (WhatsApp Business API ou SMS). Por e-mail basta o servidor de e-mail do Keycloak.

**Critérios de aceite**

- Dado um CPF sem cadastro, quando o informo, então vejo a mesma mensagem de um CPF cadastrado, e nenhum código é enviado.
- Dado o sexto código errado, então o convite é bloqueado e é preciso pedir um novo.

### CLI-02 · Ver meus veículos e contratos

**Como** associado, **quero** ver meus veículos e o que pago, **para** conferir minha proteção.

**Regras**

- Mostra os contratos não cancelados: modelo, placa, tipo, mensalidade e situação. "A conferir" aparece como "Em conferência" **(proposta)**.
- Nunca mostra perfil, potencial de indicação nem os atendimentos da equipe.
- Boletos e pagamentos aparecem como "Disponível após integração com o SGA".

**Critérios de aceite**

- Dado um cliente com duas placas, uma cancelada, então ele vê só a que não está cancelada.

### CLI-03 · Atualizar meus contatos

**Como** associado, **quero** atualizar meu WhatsApp e e-mail, **para** continuar recebendo o atendimento.

**Regras**

- O cliente altera o WhatsApp e o e-mail principais. O contato novo só vale depois de confirmado por código **(proposta)**.
- O cliente pode recusar mensagens (opt-out), com o efeito da [CAR-04](02-carteira.md#car-04--atualizar-contatos-do-cliente).
- A alteração vai para a auditoria, e a ficha mostra "atualizado pelo cliente" com a data.

**Critérios de aceite**

- Dado um WhatsApp novo não confirmado, então as filas continuam usando o número antigo.

### CLI-04 · Indicar um amigo

**Como** associado, **quero** indicar um amigo pelo celular, **para** ajudar quem conheço a se proteger.

**Regras**

- Campos: nome, WhatsApp, veículo (opcional) e a declaração obrigatória de que o amigo autorizou o contato.
- A indicação entra com canal Área do cliente, status Recebida e o próprio cliente como indicador.
- Os avisos de duplicidade da [IND-01](05-indicacoes.md#ind-01--registrar-indicação) são só para a equipe: o cliente nunca fica sabendo se o amigo já é cliente ou se já foi indicado.
- No máximo 10 indicações por dia por cliente **(proposta)**.

**Critérios de aceite**

- Dada uma indicação feita pelo cliente, então ela aparece na lista da equipe ([IND-02](05-indicacoes.md#ind-02--acompanhar-e-atualizar-indicação)) com canal Área do cliente.

### CLI-05 · Acompanhar minhas indicações

**Como** associado, **quero** ver como andam minhas indicações, **para** saber se o amigo fechou.

**Regras**

- Lista com nome do indicado, data e status. "Perdida" aparece como "Não fechou" **(proposta)**.
- Não mostra observações internas nem o contrato do amigo.

**Critérios de aceite**

- Dada uma indicação convertida pela equipe, quando o cliente abre a lista, então ela aparece como Convertida.
