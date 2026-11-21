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
- Sem uso por 30 minutos, a sessão expira. A regra vale no servidor: o token só é renovado quando a pessoa usa o CRM (mexe na tela ou faz uma ação); parada, a sessão expira no Keycloak e a API recusa o token vencido. Consultas automáticas, como os contadores do menu, não contam como uso.
- Na tela de login, quem marca "Confiar neste dispositivo" (o "Lembrar de mim" do Keycloak) continua conectado nesse dispositivo por até 15 dias, sem expirar por inatividade. Não marque em computador compartilhado; sair encerra a sessão na hora.

**Critérios de aceite**

- Dado um usuário que saiu, quando volta pelo botão Voltar do navegador, então é levado de novo ao login.
- Dado um usuário que entrou sem marcar "Confiar neste dispositivo", quando fica 30 minutos sem usar, então a sessão é encerrada.
- Dado um usuário que marcou "Confiar neste dispositivo", quando volta ao CRM no mesmo dispositivo dentro de 15 dias, então entra sem digitar a senha.

### ACE-04 · Gerenciar usuários da equipe

**Como** gerencial, **quero** criar, desativar e trocar o papel de usuários, **para** controlar quem acessa a carteira.

**Regras**

- Só o gerencial gerencia usuários. Cada pessoa da equipe tem um papel: gerencial, vendedor ou atendente.
- O usuário novo recebe por e-mail o link para definir a senha.
- Usuário não é excluído, só desativado, para o histórico manter o autor de cada atendimento.
- Ao desativar um vendedor, os contratos não cancelados dele (Ativo, A conferir e Suspenso) vão para outro vendedor ativo escolhido pelo gerencial. Se ele não escolher, os contratos ficam com o próprio gerencial que desativou, que depois os redistribui ([CAR-09](02-carteira.md#car-09--redistribuir-contratos-em-lote)). Contratos cancelados continuam com o vendedor original, como histórico.
- Contas de cliente só nascem pelo primeiro acesso ([CLI-01](08-area-do-cliente.md#cli-01--fazer-o-primeiro-acesso)), nunca por esta tela.
- O usuário técnico "Importação da planilha" não aparece nesta lista.

**Critérios de aceite**

- Dado um vendedor desativado, quando tenta entrar, então o login é recusado, e os atendimentos dele continuam no histórico com o nome dele.
- Dado um vendedor com contratos não cancelados, quando o gerencial o desativa sem escolher outro vendedor, então os contratos passam para esse gerencial, e cada troca fica na auditoria.
