---
name: nova-entidade
description: Modela um agregado, entidade, objeto de valor ou modelo de leitura do domínio (DDD) e o mapeia com Hibernate, criando a tabela via Liquibase se ela ainda não existir, com repositório Spring Data. Use ao precisar persistir ou consultar dados de uma tabela que ainda não tem @Entity, ou ao alterar o schema.
---

# Nova entidade (Liquibase + Hibernate)

Regra de ouro: **schema no Liquibase, acesso pelo Hibernate, zero SQL nativo em Java.**

A modelagem segue **DDD, SOLID e Clean Code** (`CLAUDE.md`): a entidade é um objeto com comportamento, não
um saco de getters/setters. Mudança de estado só por métodos com nome de negócio que protegem as regras.

## 0. Classifique antes de codar (DDD)

| Pergunta | Se sim |
|---|---|
| Tem identidade própria e ciclo de vida (é "o mesmo" mesmo se os dados mudarem)? | **Entidade** |
| É alterada e consultada de fora como unidade, protegendo invariantes de partes internas? | **Raiz de agregado**: ganha repositório |
| Só existe dentro de outro objeto (ex.: contato do associado)? | **Entidade interna** do agregado: sem repositório, alterada pela raiz |
| Não tem identidade, é definida pelos valores e tem regra de formato/validação (placa, telefone, CPF, e-mail, dinheiro)? | **Objeto de valor**: `record` imutável que se valida |
| Só serve para tela de consulta? | **Modelo de leitura**: projeção ou visão `@Immutable` |

Defina o **contexto** dono (tabela de contextos do `CLAUDE.md`). A classe vai em `<contexto>/entidade/` (entidades, objetos de valor, enums e eventos).
O nome vem da linguagem ubíqua (histórias e dicionário de dados).

## 1. A tabela já existe?

```bash
grep -rln 'tableName="<tabela>"\|viewName="<tabela>"' backend/src/main/resources/db/changelog/changes/
```

Com o Postgres local de pé, confira os tipos reais (o validador do Hibernate compara com eles):

```bash
docker exec crm-superauto-postgres-1 psql -U crm -d crm -c "\d <tabela>"
```

### Se precisar criar ou alterar

- **Nunca edite um changeset já aplicado.** Crie um arquivo novo `changes/0NN-<assunto>.xml` e inclua no fim de `db.changelog-master.xml`.
- Um changeset por tabela (tabela + checks + índices), `author="crm-superauto"`, com `<rollback>` explícito.
- Convenções (topo do `db.changelog-master.xml`): singular, snake_case, PK `id bigint identity`, FK `<entidade>_id`, constraints `pk_/fk_/uk_/ck_/ix_`, datas de evento `timestamptz`, datas de calendário `date`, dinheiro `numeric(12,2)`, colunas `criado_em`, `atualizado_em`, `versao`.
- `CHECK`s e índices parciais vão em `<sql>` (o Liquibase Community não tem change type para eles).
- Atualize `docs/modelo-de-dados/dicionario-de-dados.md`.

## 2. Entidade

Pacote do módulo dono do dado (`cadastro`, `relacionamento`, `comercial`…). Modelo:

```java
@Entity
@Table(name = "contrato")
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendedor_id")
    private UUID vendedorId;                    // outro agregado (outro contexto): só o id

    @OneToMany(mappedBy = "contrato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParteDoAgregado> partes = new ArrayList<>();   // dentro do agregado: associação

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private SituacaoContrato situacao;          // enum espelha o CHECK

    @Convert(converter = MensalidadeConverter.class)
    @Column(name = "valor_mensalidade", nullable = false, precision = 12, scale = 2)
    private Mensalidade mensalidade;            // objeto de valor, não BigDecimal solto

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Version
    @Column(nullable = false)
    private int versao;

    protected Contrato() {
    }

    /** Métodos de domínio no lugar de setters; efeitos em outros contextos viram eventos. */
    public void transferirPara(UUID novoVendedorId, UUID autor) {
        ...
        registerEvent(new ContratoTransferido(id, anterior, novoVendedorId, autor));
    }
}
```

Objeto de valor (modelo: `cadastro/entidade/Mensalidade.java`, `compartilhado/entidade/Email.java`):

```java
public record Placa(String valor) {
    public Placa {
        valor = Objects.requireNonNull(valor).trim().toUpperCase(Locale.ROOT).replace("-", "");
        if (!valor.matches("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$")) {
            throw NegocioException.invalido("placa-invalida", "Placa inválida.");
        }
    }
}
```

Mapeie com `AttributeConverter` (um campo) ou `@Embeddable` (vários campos). Objeto de valor usado por mais
de um contexto (e-mail, telefone, documento) fica em `compartilhado/entidade`.

Checklist:
- [ ] Construtor `protected` vazio + construtor/fábrica de domínio. Sem setters genéricos; mudanças por métodos com nome de negócio que validam a regra.
- [ ] `@Version` se a tabela tem `versao`.
- [ ] Enum por `CHECK ... IN (...)`, com comentário apontando o nome do check.
- [ ] `timestamptz` → `OffsetDateTime`; `date` → `LocalDate`; `numeric` → `BigDecimal`; `smallint` → `Short`; `jsonb` → `Map<String, Object>` com `@JdbcTypeCode(SqlTypes.JSON)`.
- [ ] FK para **outro agregado** → só o id (`UUID vendedorId`, `Long associadoId`), nunca `@ManyToOne`.
- [ ] Partes **do mesmo agregado** → `@OneToMany(cascade = ALL, orphanRemoval = true)` a partir da raiz; a lista não é exposta para alteração (devolva `List.copyOf`).
- [ ] Conceito com regra de formato (placa, telefone, documento, e-mail, dinheiro) → objeto de valor, nunca `String`/`BigDecimal` solto.
- [ ] Mudança que interessa a outro contexto (auditoria, filas) → evento de domínio (`extends AbstractAggregateRoot<...>` + `registerEvent`); lembre que o evento só é publicado no `save`/`saveAll` do repositório.
- [ ] Visão ou tabela só de leitura → `@Immutable`.
- [ ] PK atribuída (UUID do Keycloak) → implementar `Persistable<UUID>` (veja `acesso/Usuario.java`).

## 3. Repositório

- Só para **raiz de agregado**, em `<contexto>/repositorio/`. `extends JpaRepository<Raiz, Id>`; para visões/leitura, `extends Repository<Entidade, Id>` só com os métodos de consulta.
- Prefira métodos derivados (`countByVendedorIdAndSituacaoNot`). Para agregações, JPQL com projeção em `record`:
  ```java
  @Query("select new br.com.superauto.crm.cadastro.TotalPorVendedor(c.vendedor.id, count(c)) from Contrato c ... group by c.vendedor.id")
  ```
  O record da projeção deve ser **top-level** (não aninhado).
- Passe enums como parâmetro, não como literal no JPQL.
- **Proibido** `nativeQuery = true`.

## 4. Validar

Rode `cd backend && mvn -o -q test` e depois a skill **`validar-banco`**: só ela prova que `ddl-auto: validate` aceita o mapeamento e que as consultas funcionam no Postgres de verdade.
