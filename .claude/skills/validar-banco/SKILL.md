---
name: validar-banco
description: Valida entidades JPA, consultas e escrita do Hibernate contra o PostgreSQL local (docker) com um teste de integração temporário que não deixa dados para trás. Use depois de criar ou alterar entidade, repositório, consulta JPQL ou changeset Liquibase.
---

# Validar mapeamentos contra o Postgres local

Os testes unitários usam mocks e **não** provam que o mapeamento bate com o schema. Esta validação prova.

## 1. Pré-condições

```bash
docker ps --format '{{.Names}} {{.Status}}' | grep postgres     # crm-superauto-postgres-1 healthy
```

Se não estiver de pé: `docker compose up -d postgres keycloak` (o contexto Spring busca o issuer do Keycloak).

Para montar dados de teste, leia as colunas obrigatórias e os CHECKs:

```bash
docker exec crm-superauto-postgres-1 psql -U crm -d crm -c "\d <tabela>"
```

## 2. Teste temporário

Crie `backend/src/test/java/br/com/superauto/crm/TmpValidacaoIT.java`:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)   // NONE falha: SecurityConfig precisa do contexto web
@Transactional                                                          // rollback no fim
class TmpValidacaoIT {
    @Autowired JdbcTemplate jdbc;   // permitido SÓ aqui, para montar fixtures e conferir o resultado
    @Autowired EntityManager em;

    @Test
    void valida() {
        // 1. fixtures mínimas com jdbc (respeitando CHECKs e FKs)
        // 2. chamar repositórios/serviços reais
        // 3. em.flush() antes de conferir escrita — sem flush o Hibernate ainda não emitiu o SQL
        // 4. conferir com jdbc.queryForObject(...) e AssertJ
    }
}
```

O simples fato de o contexto subir já valida todas as entidades (`ddl-auto: validate`).

Rode:

```bash
cd backend && mvn -o test -Dtest=TmpValidacaoIT -Dsurefire.failIfNoSpecifiedTests=false 2>&1 \
  | grep -E "Tests run:|Caused by|Schema-validation|expected|but was" | head -30
```

## 3. Cuidados (aprendidos na prática)

- **`REQUIRES_NEW` escapa do rollback.** Código com transação própria (ex.: `ProjecaoUsuarioService`) grava de verdade. E um `DELETE` de limpeza feito com o `JdbcTemplate` dentro do teste **também é desfeito** pelo rollback. Para limpar o que foi commitado, faça a limpeza fora do teste (passo 4) ou use dados com marcador (`username` começando com `tmp.`).
- Use dados com marcador identificável (`tmp.`, placa `TMP...` quando o CHECK permitir) para achar sobras.
- Não use usuários reais de teste (`gerente`, `vendedor1`…) em operações que comitam.

## 4. Limpeza (obrigatória)

1. Apague o arquivo `TmpValidacaoIT.java`.
2. Confira que o banco voltou ao estado anterior (contagens das tabelas tocadas antes × depois) e remova sobras pelo marcador:
   ```bash
   docker exec crm-superauto-postgres-1 psql -U crm -d crm -c "select id, username from usuario where username like 'tmp.%'"
   ```
3. Rode a suíte normal: `cd backend && mvn -o -q test`.
4. Informe ao usuário o que foi validado e se algo precisou ser limpo.
