# Histórias de usuário

São 37 histórias em 9 módulos, cada uma com suas regras de negócio e critérios de aceite. As regras vêm da planilha, da macro de atendimento e das decisões do modelo de dados. Regras novas, sugeridas para o sistema web, estão marcadas com **(proposta)** e precisam da sua validação.

**Como ler:** cada história tem o formato *Como / quero / para*, as **Regras** de negócio e os **Critérios de aceite** (*Dado / quando / então*), que viram testes. As regras gerais (RG) valem para todas e não se repetem. O modelo de dados está em [`../modelo-de-dados`](../modelo-de-dados/README.md).

## Módulos

| Módulo | Arquivo | Histórias |
| --- | --- | --- |
| Acesso e perfis (ACE) | [01-acesso.md](01-acesso.md) | 4 |
| Carteira de clientes (CAR) | [02-carteira.md](02-carteira.md) | 8 |
| Painel, filas e régua (FIL) | [03-filas-e-regua.md](03-filas-e-regua.md) | 6 |
| Atendimentos (ATD) | [04-atendimentos.md](04-atendimentos.md) | 5 |
| Indicações (IND) | [05-indicacoes.md](05-indicacoes.md) | 3 |
| Inteligência comercial (INT) | [06-inteligencia.md](06-inteligencia.md) | 3 |
| Importação da planilha (IMP) | [07-importacao.md](07-importacao.md) | 2 |
| Área do cliente (CLI) | [08-area-do-cliente.md](08-area-do-cliente.md) | 5 |
| Auditoria (AUD) e fora do escopo | [09-auditoria-e-escopo.md](09-auditoria-e-escopo.md) | 1 |

## Mapa das histórias

O MVP tem 25 histórias: com elas a equipe já trabalha sem a planilha. A fase indica a semana do cronograma de 10 semanas.

| ID | História | Papéis | Fase |
| --- | --- | --- | --- |
| [ACE-01](01-acesso.md#ace-01--entrar-no-sistema) | Entrar no sistema | Todos | MVP · sem. 2 |
| [ACE-02](01-acesso.md#ace-02--ver-o-menu-conforme-o-papel) | Ver o menu conforme o papel | Gerencial, vendedor, atendente | MVP · sem. 2 |
| [ACE-03](01-acesso.md#ace-03--sair-do-sistema) | Sair do sistema | Todos | MVP · sem. 2 |
| [ACE-04](01-acesso.md#ace-04--gerenciar-usuários-da-equipe) | Gerenciar usuários da equipe | Gerencial | Gestão · sem. 7 |
| [CAR-01](02-carteira.md#car-01--buscar-e-filtrar-a-carteira) | Buscar e filtrar a carteira | Gerencial, vendedor, atendente | MVP · sem. 3–6 |
| [CAR-02](02-carteira.md#car-02--cadastrar-cliente-veículo-e-contrato) | Cadastrar cliente, veículo e contrato | Gerencial, vendedor | MVP · sem. 3–6 |
| [CAR-03](02-carteira.md#car-03--ver-a-ficha-do-cliente) | Ver a ficha do cliente | Gerencial, vendedor, atendente | MVP · sem. 6 |
| [CAR-04](02-carteira.md#car-04--atualizar-contatos-do-cliente) | Atualizar contatos do cliente | Gerencial, vendedor, atendente | MVP · sem. 6 |
| [CAR-05](02-carteira.md#car-05--alterar-situação-mensalidade-e-vendedor-do-contrato) | Alterar situação, mensalidade e vendedor do contrato | Gerencial | MVP · sem. 6 |
| [CAR-06](02-carteira.md#car-06--classificar-perfil-e-potencial-de-indicação) | Classificar perfil e potencial de indicação | Gerencial, vendedor, atendente | MVP · sem. 6 |
| [CAR-07](02-carteira.md#car-07--exportar-lista-de-clientes) | Exportar lista de clientes | Gerencial, vendedor | Gestão · sem. 7 |
| [CAR-08](02-carteira.md#car-08--anonimizar-cliente-lgpd) | Anonimizar cliente (LGPD) | Gerencial | Gestão · sem. 9 |
| [FIL-01](03-filas-e-regua.md#fil-01--ver-o-painel) | Ver o painel | Gerencial, vendedor, atendente | MVP · sem. 5 |
| [FIL-02](03-filas-e-regua.md#fil-02--trabalhar-a-fila-de-contatos-de-hoje) | Trabalhar a fila de contatos de hoje | Gerencial, vendedor, atendente | MVP · sem. 5 |
| [FIL-03](03-filas-e-regua.md#fil-03--trabalhar-a-fila-de-atrasados) | Trabalhar a fila de atrasados | Gerencial, vendedor, atendente | MVP · sem. 5 |
| [FIL-04](03-filas-e-regua.md#fil-04--ver-aniversariantes) | Ver aniversariantes | Gerencial, vendedor, atendente | MVP · sem. 5 |
| [FIL-05](03-filas-e-regua.md#fil-05--abrir-conversa-no-whatsapp) | Abrir conversa no WhatsApp | Gerencial, vendedor, atendente | MVP · sem. 5 |
| [FIL-06](03-filas-e-regua.md#fil-06--configurar-prazos-da-régua) | Configurar prazos da régua | Gerencial | MVP · sem. 3 |
| [ATD-01](04-atendimentos.md#atd-01--registrar-atendimento-de-pós-venda) | Registrar atendimento de pós-venda | Gerencial, vendedor, atendente | MVP · sem. 4–6 |
| [ATD-02](04-atendimentos.md#atd-02--registrar-atendimento-avulso) | Registrar atendimento avulso | Gerencial, vendedor, atendente | MVP · sem. 4–6 |
| [ATD-03](04-atendimentos.md#atd-03--registrar-mensagem-de-aniversário) | Registrar mensagem de aniversário | Gerencial, vendedor, atendente | MVP · sem. 4–6 |
| [ATD-04](04-atendimentos.md#atd-04--corrigir-um-atendimento) | Corrigir um atendimento | Gerencial, autor do registro | MVP · sem. 6 |
| [ATD-05](04-atendimentos.md#atd-05--consultar-o-histórico-de-atendimentos) | Consultar o histórico de atendimentos | Gerencial, vendedor, atendente | MVP · sem. 6 |
| [IND-01](05-indicacoes.md#ind-01--registrar-indicação) | Registrar indicação | Gerencial, vendedor, atendente | MVP · sem. 4 |
| [IND-02](05-indicacoes.md#ind-02--acompanhar-e-atualizar-indicação) | Acompanhar e atualizar indicação | Gerencial, vendedor | MVP · sem. 4 |
| [IND-03](05-indicacoes.md#ind-03--converter-indicação-em-contrato) | Converter indicação em contrato | Gerencial, vendedor | MVP · sem. 4 |
| [INT-01](06-inteligencia.md#int-01--ver-indicadores-da-carteira) | Ver indicadores da carteira | Gerencial, vendedor | Gestão · sem. 7 |
| [INT-02](06-inteligencia.md#int-02--ver-desempenho-por-cidade-e-tipo-de-veículo) | Ver desempenho por cidade e tipo de veículo | Gerencial, vendedor | Gestão · sem. 7 |
| [INT-03](06-inteligencia.md#int-03--ver-indicações-por-associado) | Ver indicações por associado | Gerencial, vendedor | Gestão · sem. 7 |
| [IMP-01](07-importacao.md#imp-01--importar-a-planilha) | Importar a planilha | Gerencial | MVP · sem. 4 |
| [IMP-02](07-importacao.md#imp-02--consultar-resultado-da-importação-e-base-histórica) | Consultar resultado da importação e base histórica | Gerencial | MVP · sem. 4 |
| [CLI-01](08-area-do-cliente.md#cli-01--fazer-o-primeiro-acesso) | Fazer o primeiro acesso | Cliente | Área do cliente · sem. 8 |
| [CLI-02](08-area-do-cliente.md#cli-02--ver-meus-veículos-e-contratos) | Ver meus veículos e contratos | Cliente | Área do cliente · sem. 8 |
| [CLI-03](08-area-do-cliente.md#cli-03--atualizar-meus-contatos) | Atualizar meus contatos | Cliente | Área do cliente · sem. 8 |
| [CLI-04](08-area-do-cliente.md#cli-04--indicar-um-amigo) | Indicar um amigo | Cliente | Área do cliente · sem. 8 |
| [CLI-05](08-area-do-cliente.md#cli-05--acompanhar-minhas-indicações) | Acompanhar minhas indicações | Cliente | Área do cliente · sem. 8 |
| [AUD-01](09-auditoria-e-escopo.md#aud-01--consultar-a-trilha-de-auditoria) | Consultar a trilha de auditoria | Gerencial | Gestão · sem. 9 |

## Regras gerais

Estas regras valem para todas as histórias e não são repetidas em cada uma.

| ID | Regra |
| --- | --- |
| RG-01 | **Data de hoje.** "Hoje" é sempre a data de São Paulo (America/Sao\_Paulo), qualquer que seja o fuso do servidor ou do navegador. |
| RG-02 | **Formatos.** Datas em dd/mm/aaaa, valores em R$ 1.234,56, telefone como (37) 9 9999-9999 e placa em maiúsculas sem hífen. |
| RG-03 | **Visibilidade por papel.** Gerencial e atendente veem toda a carteira. O vendedor vê apenas os contratos em que é o vendedor, em todas as telas (filas, carteira, ficha, inteligência, exportação). O cliente vê apenas os próprios dados. |
| RG-04 | **CPF/CNPJ.** O atendente vê o documento mascarado (\*\*\*.\*\*\*.418-\*\*). Gerencial e vendedor veem completo ao clicar em "mostrar", e cada exibição é registrada na auditoria **(proposta)**. |
| RG-05 | **Busca.** Nome sem diferença de acento ou maiúsculas ("jose" encontra "José" e vice-versa), por trecho do nome. Também por placa, CPF/CNPJ ou telefone, com ou sem pontuação. |
| RG-06 | **Edição simultânea.** Se duas pessoas alteram a mesma ficha, a segunda gravação é recusada com a mensagem "Este cadastro foi alterado por outra pessoa. Recarregue para ver a versão atual." Nada é sobrescrito em silêncio. |
| RG-07 | **Nada se apaga.** Contratos e atendimentos nunca são excluídos: contrato muda de situação, atendimento é corrigido por outro registro. |
| RG-08 | **Auditoria.** Toda alteração de cliente, contato, contrato, régua, indicação e configuração é registrada com quem, quando, valor anterior e valor novo. |
| RG-09 | **Mensalidade não é receita.** Toda tela com valores de mensalidade mostra o aviso de que são valores contratados, não recebidos. Recebimento só com integração ao SGA. |
| RG-10 | **WhatsApp.** O sistema abre a conversa com o número, mas nunca envia mensagem sozinho. |
| RG-11 | **Finalidade dos dados.** Dados pessoais só são usados para relacionamento e pós-venda, e contatos com opt-out não são oferecidos para mensagem. |
| RG-12 | **Situações do contrato.** A conferir (padrão de todo contrato novo), Ativo, Suspenso e Cancelado. Suspensos e cancelados ficam fora das filas e dos aniversariantes. |
| RG-13 | **Régua de pós-venda.** D+7 → D+30 → D+60 → D+90 → Trimestral (repete). Prazos atuais: 7 dias após a adesão, depois 23, 30, 30 e 90 dias após o contato anterior concluído. |
| RG-14 | **Próximo contato.** Se houver retorno manual, ele vale. Senão: data do último contato de pós-venda concluído + prazo da etapa pendente. Sem contato concluído: data de adesão + prazo do D+7. |
