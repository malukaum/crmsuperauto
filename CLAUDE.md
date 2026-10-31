# CRM Super Auto · Guideline de desenvolvimento

CRM de pós-venda da Regional Formiga (proteção veicular), substituindo uma planilha Excel/VBA.
Tudo — código, nomes, mensagens, commits e documentação — é em **português do Brasil**.

## Princípios obrigatórios: DDD, SOLID e Clean Code

**O projeto é modelado com Domain-Driven Design (DDD, Design Orientado a Domínio), a arquitetura é SOLID e
todo código deve seguir Clean Code.** Os três se complementam:

| Princípio | Responde a | Escala |
|---|---|---|
| **DDD** | *O que* modelar e *onde* fica: linguagem do negócio, contextos, agregados, fronteiras | Sistema e módulos |
| **SOLID** | *Como* desenhar as classes e as dependências entre elas | Classes e interfaces |
| **Clean Code** | *Como* escrever cada método, nome e teste | Linhas de código |

Isso vale para backend e frontend, para código novo e para qualquer trecho alterado. Código que não segue
estes princípios não está pronto, mesmo que funcione e os testes passem. Na dúvida entre "mais rápido" e
"mais limpo", escolha o limpo. Se uma tarefa exigir abrir exceção, explique o motivo ao usuário antes de seguir.

### DDD — Design Orientado a Domínio

**Linguagem ubíqua.** O código fala a língua do negócio, a mesma das histórias (`docs/historias`) e do
dicionário de dados: *associado, contrato vigente, carteira, régua, etapa, acompanhamento, próximo contato,
fila, atendimento, indicação, conversão, anonimização*. Classes, métodos, endpoints, telas e testes usam esses
termos, sem tradução para jargão técnico ("cliente" é o papel de acesso; a pessoa da carteira é **associado**).
Termo novo ou ambíguo: alinhe com o usuário e registre na história antes de codar.

**Contextos delimitados (bounded contexts).** Cada contexto é um pacote de primeiro nível com modelo próprio:

| Contexto | Pacote | Histórias | Agregados (raiz) | Observação |
|---|---|---|---|---|
| Acesso | `acesso` | ACE | `Usuario` (projeção local) | Contas e papéis vivem no Keycloak, atrás da camada anticorrupção `ProvedorIdentidade` |
| Cadastro / Carteira | `cadastro` | CAR | `Associado` (com `ContatoAssociado`), `Veiculo`, `Contrato` | Núcleo do domínio; o contrato liga associado e veículo |
| Relacionamento | `relacionamento` | FIL, ATD | `AcompanhamentoContrato`, `Atendimento` | Régua de pós-venda, filas e atendimentos |
| Comercial | `comercial` | IND, CLI-01 | `Indicacao`, `ConviteAcesso` | |
| Importação | `importacao` | IMP | `Importacao` (com `ImportacaoRegistro`) | Camada anticorrupção da planilha: o formato da planilha não entra no domínio |
| Auditoria | `auditoria` | AUD, RG-08 | `EventoAuditoria` | Só inserção |
| Inteligência | `inteligencia` | INT | — | Só modelos de leitura (consultas e visões) |
| Área do cliente | `areacliente` | CLI | — | Só camada de aplicação, compondo os outros contextos para o papel cliente |
| Referência | `referencia` | — | `UnidadeFederativa`, `Municipio`, `TipoVeiculo`, `TipoContato`, `EtapaRelacionamento` | Núcleo compartilhado (*shared kernel*), quase estático |
| Compartilhado | `compartilhado` | — | — | Núcleo compartilhado de objetos de valor usados por vários contextos (`Email`; depois `Telefone`, `Documento`) |

**Camadas dentro de cada contexto:** cada contexto tem as mesmas camadas, e nenhuma camada é global (não
existe um pacote `entidade/` com as entidades de todos os contextos — isso apagaria as fronteiras do DDD).

```
<contexto>/
  entidade/         modelo de domínio: entidades e raízes de agregado, objetos de valor, enums,
                    eventos de domínio e conversores JPA
  repositorio/      repositórios Spring Data (um por raiz de agregado), projeções de consulta e
                    portas de acesso a dados externos ou de outro contexto (ex.: ProvedorIdentidade)
  servico/          serviços de domínio (regras puras, sem anotação do Spring) e serviços de aplicação
                    (um método = um caso de uso, @Service), comandos, resultados e listeners de eventos
  infraestrutura/   adaptadores: implementações de portas (keycloak/, persistencia/, planilha/...)
  api/              controllers REST e records de request do contexto
```

A dependência só aponta para dentro: `api → servico → repositorio → entidade`, e a `infraestrutura`
implementa as interfaces do `repositorio`. A `entidade` não depende de nenhuma outra camada nem de framework
web; o `servico` não depende de `api` nem de `infraestrutura`.

Dentro de `servico/`, mantenha a distinção do DDD pelo papel da classe: **serviço de domínio** é uma classe
pura (sem Spring, testada sem mocks, registrada por `@Bean` em `infraestrutura/<Contexto>Config`), e **serviço
de aplicação** é `@Service`, orquestra e não contém regra de negócio.

**Blocos de construção:**

- **Agregado:** grupo de objetos alterado como uma unidade, com uma **raiz** que protege as invariantes. De fora, só a raiz é acessada; as partes internas mudam por métodos da raiz (`associado.adicionarContato(...)`, não `contatoRepository.save(...)`).
- **Um repositório por raiz de agregado**, nunca para partes internas.
- **Entre agregados, referência por id**, nunca por objeto (`UUID vendedorId`, `Long associadoId`). Associações JPA (`@OneToMany`, `@ManyToOne`) só **dentro** do mesmo agregado.
- **Uma transação altera, de preferência, um agregado.** Quando um caso de uso precisar alterar vários (ex.: transferir a carteira de um vendedor), o serviço de aplicação coordena isso de forma explícita.
- **Objetos de valor** para conceitos sem identidade e com regra própria: `Placa`, `Telefone`, `Documento` (CPF/CNPJ), `Email`, `Mensalidade`. São `record`s imutáveis que **se validam na criação** e são mapeados por `@Embeddable` ou `AttributeConverter`. É proibido representar esses conceitos como `String`/`BigDecimal` soltos (obsessão por primitivos).
- **Entidade rica:** a regra de negócio que muda o estado mora na entidade ou no objeto de valor, não no serviço.
- **Serviço de domínio:** regra que envolve mais de um agregado e não cabe em nenhum deles (ex.: `TransferenciaCarteira`). Sem estado e sem Spring, no pacote `servico`.
- **Serviço de aplicação:** orquestra o caso de uso (carrega, chama o domínio, salva, audita, controla a transação e o escopo por papel). É fino: não contém regra de negócio.
- **Eventos de domínio** para efeitos em outro contexto (ex.: `ContratoTransferido` → auditoria). São `record`s na `entidade` do contexto que publica, com nome no passado. Quem publica é a raiz do agregado (`AbstractAggregateRoot` + `registerEvent`, publicado no `save`) ou, quando a mudança acontece fora de um agregado local (ex.: conta no Keycloak), o serviço de aplicação via `ApplicationEventPublisher`. Quem reage usa `@TransactionalEventListener(phase = BEFORE_COMMIT)`, para gravar na mesma transação; adicione `fallbackExecution = true` quando o evento puder nascer fora de transação.
- **Camada anticorrupção:** todo sistema externo (Keycloak, planilha, futuramente o SGA) entra por uma porta do domínio; o modelo externo é traduzido no adaptador e nunca vaza.
- **Modelos de leitura:** telas de consulta (filas, painéis, inteligência) podem ler projeções, visões `@Immutable` e DTOs direto, sem carregar agregados (CQRS leve). Escrita sempre passa pelo agregado.

**Comunicação entre contextos:** um contexto **não** acessa entidades nem repositórios de outro. Ele declara
uma porta no próprio `repositorio` (ex.: `acesso` declara `ContratosDoVendedor`) e o contexto dono do dado a
implementa na sua `infraestrutura`. Outra opção são eventos de domínio.

### SOLID

| Princípio | Como se aplica aqui |
|---|---|
| **S** — Responsabilidade única | Cada classe tem um motivo para mudar. Serviço de aplicação orquestra; regra mora na entidade, no objeto de valor ou no serviço de domínio; controller só traduz HTTP; adaptador só fala com o sistema externo. |
| **O** — Aberto/fechado | Comportamento novo entra por nova implementação ou novo valor de enum, não por `if`/`switch` espalhado em várias classes. |
| **L** — Substituição de Liskov | Toda implementação de uma porta cumpre o contrato documentado na interface (inclusive quais exceções lança), e pode substituir a outra sem o chamador perceber. |
| **I** — Segregação de interfaces | Interfaces pequenas, com só o que o cliente usa. Portas desenhadas pelo contexto consumidor; repositórios de leitura estendem `Repository`, não `JpaRepository`. |
| **D** — Inversão de dependência | `entidade` e `servico` dependem de abstrações (repositórios e portas como `ProvedorIdentidade`), nunca de Keycloak, `RestClient`, `HttpStatus` ou `Jwt`. Implementações ficam em `infraestrutura`. |

### Clean Code

- **Nomes que revelam intenção**, na linguagem ubíqua (`contratosVigentes`, `transferirPara`, `exigirPapelDaEquipe`). Nada de `dados2`, `aux`, `tmp`, `flag`, abreviações obscuras ou nomes genéricos como `Manager`/`Helper`/`Utils`.
- **Funções pequenas que fazem uma coisa**, num só nível de abstração. Extraia quando um método passar de ~20 linhas ou precisar de comentário para separar blocos.
- **Poucos parâmetros:** até 3. Mais que isso, agrupe num `record` com significado (`NovoUsuario`, `NovaConta`).
- **Sem efeitos colaterais escondidos.** Método que consulta não altera estado; método que altera tem nome de verbo de ação.
- **Sem números e textos mágicos.** Use constantes nomeadas ou enums (`SituacaoContrato.CANCELADO`, não `"CANCELADO"`).
- **Sem `null` como regra de negócio:** use `Optional` no retorno de buscas e falhe cedo com `NegocioException`.
- **Erros por exceção com significado**, nunca por código de retorno. Não engula exceção sem log e sem motivo explícito.
- **Comentários explicam o porquê, não o quê.** Se o comentário descreve o que o código faz, renomeie ou extraia em vez de comentar. Javadoc curto em classes públicas e portas, dizendo a responsabilidade e as regras (RG/história) que implementam.
- **Sem código morto, comentado ou duplicado** (DRY): apague; o git guarda o histórico.
- **Lombok** só para tirar código repetitivo: `@Getter`, `@RequiredArgsConstructor`, `@NoArgsConstructor(access = PROTECTED)` nas entidades. `@Data` e `@Setter` são proibidos (erro de compilação via `backend/lombok.config`), porque estado muda só por métodos de domínio; evite `@EqualsAndHashCode`/`@ToString` em entidades JPA. Getter que devolve `Optional` ou cópia imutável continua escrito à mão; `record` não precisa de Lombok.
- **Imutabilidade por padrão:** `record` para DTOs e objetos de valor, campos `final`, coleções devolvidas sem permitir alteração externa.
- **Testes limpos:** um conceito por teste, `@DisplayName` descrevendo a regra, estrutura dado/quando/então visível.
- **Regra do escoteiro:** ao tocar num arquivo, deixe-o um pouco melhor do que encontrou, sem desviar do escopo pedido.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21 · Spring Boot 4.1 · Spring Security 7 (resource server JWT) · Spring Data JPA / **Hibernate 7.4** · Liquibase · Maven · Lombok · Apache POI (exportação .xlsx) |
| Frontend | Angular 22 (standalone, signals, zoneless, control flow) · Angular Material 3 · SCSS · Vitest |
| Identidade | Keycloak 26 (realm `crm`) |
| Banco | PostgreSQL 16 |

Fonte da verdade do schema: **Liquibase** (`backend/src/main/resources/db/changelog`). Não há Flyway.

## Onde está o quê

- `docs/historias/` — histórias (ACE, CAR, FIL, ATD, IND, INT, IMP, CLI, AUD) e as **regras gerais RG-01…RG-14**. Toda funcionalidade nasce de uma história, e a história é a fonte da linguagem ubíqua.
- `docs/modelo-de-dados/` — modelo e dicionário de dados.
- `backend/src/main/java/br/com/superauto/crm/<contexto>/{entidade,repositorio,servico,infraestrutura,api}` — um pacote por contexto delimitado (tabela acima).
- Pacotes técnicos transversais, fora dos contextos: `erro/` (exceções e `GlobalExceptionHandler`), `seguranca/` (papéis e leitura do token), `config/`.
- `frontend/src/app/core/api/` — clientes HTTP. `paginas/<contexto>/` — telas, agrupadas pelo mesmo contexto do backend. `shared/` — componentes reutilizáveis. `layout/` — casca interna e do cliente.

## Estado atual × alvo

Backend e frontend estão no formato alvo. Referências vivas de cada padrão:

| Padrão | Onde ver |
|---|---|
| Agregado com objeto de valor, invariantes e evento | `cadastro/entidade/Contrato` (`Mensalidade`, `transferirPara`, `ContratoTransferido`) |
| Objeto de valor compartilhado + `AttributeConverter` | `compartilhado/entidade/Email` |
| Serviço de domínio | `acesso/servico/TransferenciaCarteira` (registrado em `acesso/infraestrutura/AcessoConfig`) |
| Serviço de aplicação publicando eventos (comandos) | `acesso/servico/GestaoUsuariosService` |
| Leitura separada dos comandos | `acesso/servico/ConsultaEquipeService` |
| Comando/dados agrupados em `record` (até 3 parâmetros) | `cadastro/entidade/Adesao`, `acesso/servico/DadosProjecao`, `auditoria/entidade/Alteracao` |
| Porta do consumidor implementada pelo dono do dado | `acesso/repositorio/ContratosDoVendedor` ← `cadastro/infraestrutura/persistencia/ContratosDoVendedorJpa` |
| Camada anticorrupção | `acesso/infraestrutura/keycloak` (`KeycloakAdminClient`, `TokenDeServicoKeycloak`, `ErrosKeycloak`) |
| Reação a eventos de outro contexto | `auditoria/servico/AuditoriaDeContratos`, `AuditoriaDeUsuarios` |
| Modelo de leitura | `relacionamento/entidade/PosicaoFila` (`@Immutable` sobre `vw_fila_contato`) |

Pendência conhecida: `Placa`, `Telefone` e `Documento` nascem junto com o mapeamento de `Associado` e `Veiculo` (CAR-02).

## Persistência: Hibernate, sem SQL nativo

- **Proibido no código Java:** `nativeQuery = true`, `JdbcTemplate`, `NamedParameterJdbcTemplate`, `createNativeQuery`. Use entidades, métodos derivados do Spring Data e JPQL/HQL.
- Escrita por **dirty checking**: carregue a raiz do agregado numa transação e chame um método de domínio (`contrato.transferirPara(...)`, `usuario.definirAtivo(...)`). Nada de setters públicos genéricos.
- Por pragmatismo, as classes de `entidade` levam as anotações JPA (e `AbstractAggregateRoot`), e as interfaces de `repositorio` estendem Spring Data. São as únicas dependências de framework aceitas nessas duas camadas.
- Entidades:
  - Construtor `protected` sem argumentos para o Hibernate e construtor/fábrica de domínio que garante o estado válido.
  - `@Version` em toda tabela com coluna `versao` (RG-06, edição simultânea).
  - `@CreationTimestamp` / `@UpdateTimestamp` em `criado_em` / `atualizado_em`.
  - Enums com `@Enumerated(EnumType.STRING)`, espelhando o `CHECK` da coluna.
  - Objetos de valor com `@Embeddable` (vários campos) ou `AttributeConverter` (um campo).
  - `jsonb` com `@JdbcTypeCode(SqlTypes.JSON)`.
  - Views e tabelas só de leitura com `@Immutable` e repositório estendendo `Repository` (sem `save`).
  - Id gerado pelo Keycloak (UUID atribuído) → implemente `Persistable` para o `save` fazer INSERT direto.
  - Associações JPA só **dentro** do agregado (`@OneToMany(cascade = ALL, orphanRemoval = true)` da raiz para as partes). Entre agregados, só o id.
- `spring.jpa.hibernate.ddl-auto` é **`validate`**: o Hibernate confere as entidades contra o schema na subida. Nunca use `update`/`create`.
- SQL só existe no **Liquibase**: tabelas, `CHECK`s, índices parciais e views (regras de cálculo como `vw_fila_contato`).
- Changesets já aplicados **nunca são editados**; toda mudança é um changeset novo, com `rollback`. Siga as convenções do topo de `db.changelog-master.xml`.

## Regras transversais (sempre)

- **Hoje = America/Sao_Paulo** (RG-01). Use o `Clock` injetado, nunca `LocalDate.now()` sem relógio.
- **Escopo por papel** (RG-03): vendedor só vê os próprios contratos — em toda consulta nova, filtre por vendedor quando o papel for `vendedor`.
- **Auditoria** (RG-08): toda alteração de cliente, contato, contrato, régua, indicação e configuração gera evento de auditoria com valor anterior e novo, na mesma transação. O contexto que altera publica um **evento de domínio**; o contexto `auditoria` o ouve e grava com `EventoAuditoria.criacao(...)` / `EventoAuditoria.alteracao(...)`. Nenhum outro contexto chama a auditoria direto.
- **Nada se apaga** (RG-07): contratos e atendimentos mudam de situação ou são corrigidos, nunca excluídos. Usuário é desativado, não excluído.
- **Erros:** regra de negócio lança `NegocioException` com `Tipo` (`NAO_ENCONTRADO`, `CONFLITO`, `INVALIDO`), um `codigo` kebab-case para o front e mensagem em português para o usuário final. Falha de integração lança `IntegracaoIndisponivelException`. Quem converte para HTTP (ProblemDetail) é só o `GlobalExceptionHandler`.
- **Segurança:** todo endpoint tem `@PreAuthorize` com as constantes de `Papeis`. Rotas: `/api/interno/**` (equipe), `/api/gestao/**` (gerencial), `/api/cliente/**` (cliente).
- **Validação de entrada** com Bean Validation nos records de request do controller, mensagens em português. Invariantes de negócio são validadas no domínio (objetos de valor e entidades), não só na borda.

## Frontend

- Pastas e nomes seguem os mesmos contextos e a mesma linguagem ubíqua do backend (`paginas/cadastro/ficha-associado`, não `paginas/clientes/detalhe`).
- Componentes standalone, `ChangeDetectionStrategy.OnPush`, estado em `signal`/`computed`, `inject()` em vez de construtor.
- Template com control flow (`@if`, `@for`), arquivos `.ts` / `.html` / `.scss` separados por página.
- Cliente HTTP em `core/api/<nome>-api.service.ts`, com as interfaces TypeScript espelhando os DTOs do backend.
- Rotas novas em `app.routes.ts` com `loadComponent`, `title` com o sufixo `· CRM Super Auto` e `papelGuard` + `data.papeis`.
- Tema e estilos globais em `src/styles/` (`_tokens`, `_tema`, `_base`, `_pagina`, `_conteudo`, `_tabela`, `_formulario`): cores só por token (`--crm-azul-marca`, `--crm-amarelo`...), estilo usado por mais de uma tela vira classe `crm-*` global, e o `.scss` da tela guarda só o que é dela.
- Identidade visual (logo Super Auto · Linha Truck): imagens em `public/marca/`, componentes `app-marca` e `app-logo-completa`; o tema de login do Keycloak repete os tokens.
- Formatos RG-02: datas dd/mm/aaaa, R$ 1.234,56, telefone (37) 9 9999-9999, placa maiúscula sem hífen.
- Erros da API chegam como ProblemDetail: use `detail` para a mensagem e `codigo` para decidir o fluxo.

## Testes

- Backend: JUnit 5 + AssertJ + Mockito, `@DisplayName` em português descrevendo a regra. Os **critérios de aceite** da história viram testes.
- **Domínio testado sem Spring e sem framework de mock:** entidades, objetos de valor e serviços de domínio são testados diretamente; portas viram dublês simples em memória (veja `TransferenciaCarteiraTest`).
- Serviços de aplicação testados com repositórios e portas simulados (mock de `ProvedorIdentidade`, `ContratosDoVendedor`…).
- Todo endpoint novo entra em `SegurancaEndpointsTest` (200/401/403 por papel).
- Mapeamento JPA novo ou alterado: valide contra o Postgres local com a skill `validar-banco` antes de concluir.
- Frontend: Vitest (`*.spec.ts`).

## Comandos

```bash
docker compose up postgres keycloak mailpit          # infraestrutura para dev
cd backend  && mvn -o -q test                        # testes do backend (offline)
cd backend  && mvn verify                            # build completo
cd frontend && npx ng build                          # build do front
cd frontend && npx ng test --watch=false             # testes do front
```

## Skills do projeto (`.claude/skills/`)

| Skill | Quando usar |
|---|---|
| `implementar-historia` | Implementar uma história (ex.: "implementa a CAR-02") de ponta a ponta |
| `nova-entidade` | Modelar agregado, entidade ou objeto de valor (Liquibase + Hibernate + repositório) |
| `novo-endpoint` | Caso de uso (serviço de aplicação) + controller + segurança + testes no backend |
| `nova-tela` | Página Angular nova consumindo a API |
| `validar-banco` | Conferir mapeamentos e consultas contra o Postgres local sem sujar o banco |
| `revisar-convencoes` | Checklist de DDD, SOLID, Clean Code e regras do projeto antes de commit/PR |

## Ao terminar uma tarefa

- Confira o código alterado contra **DDD, SOLID e Clean Code** (skill `revisar-convencoes`) antes de dar por concluído.
- Rode os testes afetados (backend e/ou frontend) e informe o resultado real.
- Regra de negócio nova ou alterada → atualize a história em `docs/historias` no mesmo commit.
- Mudança de schema → changeset novo e dicionário de dados atualizado.
- Commit só quando pedido. Mensagem em português, curta, no estilo do histórico (`01 Acesso`, `Busca de associado sem acento…`).
