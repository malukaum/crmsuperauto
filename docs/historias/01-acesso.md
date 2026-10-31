# Acesso e perfis (ACE)

> [Índice das histórias](README.md) · Regras gerais: [RG-01 a RG-14](README.md#regras-gerais) · [Modelo de dados](../modelo-de-dados/README.md)

A identidade e os papéis ficam no Keycloak, com quatro papéis: gerencial, vendedor, atendente e cliente.

### ACE-01 · Entrar no sistema

**Como** membro da equipe ou cliente, **quero** entrar com meu usuário e senha, **para** acessar o que o meu papel permite.

**Regras**

- O login acontece na tela do Keycloak, com o tema visual do CRM e em português.
- Depois do login, o cliente vai para a Área do cliente; os demais papéis vão para o Painel.
- Após várias senhas erradas seguidas, a conta é bloqueada temporariamente.
- "Esqueci minha senha" envia um link de troca ao e-mail cadastrado no Keycloak.
- A sessão se renova sozinha enquanto o usuário estiver usando o sistema.
- No primeiro login de cada pessoa da equipe, o sistema cria a projeção local do usuário (id, username, nome).

**Critérios de aceite**

- Dado o usuário vendedor1, quando ele entra, então vê o Painel com os números só dos contratos dele.
- Dado o usuário cliente1, quando ele entra, então vê a Área do cliente e não consegue abrir nenhuma rota interna.
- Dado um usuário sem nenhum dos quatro papéis, quando ele entra, então vê "Acesso negado".

### ACE-02 · Ver o menu conforme o papel

**Como** membro da equipe, **quero** ver só as opções que posso usar, **para** não esbarrar em telas proibidas.

**Regras**

- Grupos do menu: Atendimento (Painel, Contatos de hoje, Atrasados, Aniversariantes), Relacionamento (Carteira, Atendimentos, Indicações) e Gestão (Inteligência, Importação, Configurações, Auditoria).
- Gestão aparece só para gerencial; Inteligência também aparece para o vendedor.
- Contatos de hoje e Atrasados mostram a quantidade ao lado do nome.
- Esconder o menu não é a proteção: a API recusa com 403 qualquer chamada fora do papel.

**Critérios de aceite**

- Dado o atendente, quando abre o menu, então não vê Inteligência, Importação, Configurações nem Auditoria.
- Dado o atendente, quando digita o endereço de Configurações, então vê "Acesso negado".

### ACE-03 · Sair do sistema

**Como** usuário, **quero** sair, **para** que ninguém use minha sessão num computador compartilhado.

**Regras**

- Sair encerra a sessão no Keycloak, não só no navegador.
- Sem uso por 30 minutos, a sessão expira **(proposta)**.

**Critérios de aceite**

- Dado um usuário que saiu, quando volta pelo botão Voltar do navegador, então é levado de novo ao login.

### ACE-04 · Gerenciar usuários da equipe

**Como** gerencial, **quero** criar, desativar e trocar o papel de usuários, **para** controlar quem acessa a carteira.

**Regras**

- Só o gerencial gerencia usuários. Cada pessoa da equipe tem um papel: gerencial, vendedor ou atendente.
- O usuário novo recebe por e-mail o link para definir a senha.
- Usuário não é excluído, só desativado, para o histórico manter o autor de cada atendimento.
- Antes de desativar um vendedor com contratos, o sistema pede para transferi-los a outro vendedor **(proposta)**.
- Contas de cliente só nascem pelo primeiro acesso ([CLI-01](08-area-do-cliente.md#cli-01--fazer-o-primeiro-acesso)), nunca por esta tela.
- O usuário técnico "Importação da planilha" não aparece nesta lista.

**Critérios de aceite**

- Dado um vendedor desativado, quando tenta entrar, então o login é recusado, e os atendimentos dele continuam no histórico com o nome dele.
