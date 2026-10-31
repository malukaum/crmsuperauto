# Documentação do CRM Super Auto

CRM de relacionamento e pós-venda da Regional Formiga, que substitui a planilha `CRM_DESENVOLVIMENTO_LIMPO.xlsm`. Stack: Angular 19, Spring Boot 3 (Java 21), PostgreSQL 16 e Keycloak 26.

| Documento | Conteúdo |
| --- | --- |
| [Histórias de usuário](historias/README.md) | As 37 histórias, com regras de negócio e critérios de aceite, divididas em 9 módulos, mais as 14 regras gerais |
| [Modelo de dados](modelo-de-dados/README.md) | Diagrama, decisões de normalização, cálculo da régua, mapeamento da planilha e convenções |
| [Dicionário de dados](modelo-de-dados/dicionario-de-dados.md) | Cada tabela, coluna, restrição e índice, gerado a partir do banco |

## Onde fica cada coisa no repositório

| Caminho | O que é |
| --- | --- |
| `backend/src/main/resources/db/migration/V2__modelo_dominio.sql` | Migration Flyway do modelo de domínio |
| `scripts/db/teste_modelo.sql` | Teste das restrições do banco (só em banco de teste) |
| `scripts/db/gerar_dicionario.sql` | Gera o dicionário de dados a partir do banco |

## Regras para manter esta documentação

- Uma regra de negócio nova ou alterada muda a história correspondente no mesmo pull request do código.
- Regras marcadas **(proposta)** ainda não foram validadas pelo negócio. Ao validar, remova a marcação; ao rejeitar, corrija a regra.
- Mudança no banco é sempre uma migration nova, com `COMMENT ON` nas tabelas e colunas novas, e o dicionário de dados gerado de novo.
- Os critérios de aceite das histórias são a base dos testes de aceitação.
