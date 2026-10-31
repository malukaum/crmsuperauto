---
name: novo-endpoint
description: Cria um caso de uso no backend com endpoint REST, seguindo DDD — serviço de aplicação, regras no domínio, portas para dependências externas, controller com @PreAuthorize e validação, erros em ProblemDetail, auditoria e testes (unitário + segurança). Use ao adicionar ou alterar uma operação da API.
---

# Novo caso de uso + endpoint

**Obrigatório:** DDD, SOLID e Clean Code (seção "Princípios obrigatórios" do `CLAUDE.md`). Na prática:
um caso de uso por método do serviço de aplicação, com nome de verbo da linguagem ubíqua; **regra de negócio
no domínio** (entidade, objeto de valor ou serviço de domínio), nunca no serviço de aplicação nem no controller;
dependências externas só por interface; métodos curtos, sem números/textos mágicos, até 3 parâmetros.

## 1. Onde fica

| Peça | Local | Exemplo |
|---|---|---|
| Serviço de aplicação (casos de uso, `@Service`) | `crm/<contexto>/servico/` | `acesso/servico/GestaoUsuariosService` |
| Regra pura / serviço de domínio (sem Spring) | `crm/<contexto>/servico/` | `acesso/servico/RegrasEquipe`, `acesso/servico/TransferenciaCarteira` |
| Porta (dependência externa ou de outro contexto) | interface em `crm/<contexto>/repositorio/` | `acesso/repositorio/ProvedorIdentidade`, `acesso/repositorio/ContratosDoVendedor` |
| Adaptador | `crm/<contexto>/infraestrutura/<tecnologia>/` | `acesso/infraestrutura/keycloak/KeycloakAdminClient`; `cadastro/infraestrutura/persistencia/ContratosDoVendedorJpa` (o dono do dado implementa a porta do consumidor) |
| Controller | `crm/<contexto>/api/` | `acesso/api/UsuariosEquipeController` |
| Reação em outro contexto | listener em `crm/<outro>/servico/` | `auditoria/servico/AuditoriaDeContratos` ouve `ContratoTransferido` |

## 2. Serviço de aplicação

- Fino: carrega o agregado, chama o método de domínio, salva, cuida da transação e do escopo. Se aparecer `if` de regra de negócio aqui, mova para o domínio.
- Não acessa repositório nem entidade de **outro contexto**: use uma porta do próprio `repositorio` ou um evento de domínio.
- Depende de interfaces (portas) e repositórios Spring Data; nunca de `HttpStatus`, `Jwt`, `RestClient`.
- Recebe o autor como `UUID autor` (o controller extrai do token).
- `@Transactional` nos métodos que escrevem; escrita por dirty checking (método de domínio na entidade carregada).
- **Escopo por papel (RG-03):** se o vendedor só pode ver os próprios dados, o serviço recebe `Optional<UUID> vendedor` (vazio = vê tudo) e filtra. Veja `ContadoresFilasController` + `ContadoresFilasService`.
- **Auditoria (RG-08):** não chame a auditoria. Publique um **evento de domínio** (`record` no passado, na `entidade` do contexto: pela raiz do agregado com `registerEvent` ou pelo serviço com `ApplicationEventPublisher`) e crie/estenda o listener em `auditoria/servico/AuditoriaDe<Algo>` com `@TransactionalEventListener(phase = BEFORE_COMMIT)`, gravando `EventoAuditoria.alteracao(ItemAuditado.de(...), autor, new Alteracao(anterior, novo))` só com os campos que mudaram.
- **Erros:** `NegocioException.naoEncontrado(...)`, `.conflito(codigo, msg[, dados])`, `.invalido(codigo, msg)`. `codigo` em kebab-case (o front decide por ele); mensagem em português, dirigida ao usuário final. `dados` para números que a tela precisa (ex.: `contratosVigentes`).
- Edição simultânea (RG-06): o `@Version` já recusa; traduza `ObjectOptimisticLockingFailureException` para `NegocioException.conflito("edicao-simultanea", "Este cadastro foi alterado por outra pessoa. Recarregue para ver a versão atual.")` se ainda não houver tratamento global.
- Data de hoje sempre pelo `Clock` injetado (RG-01).

## 3. Controller

```java
@RestController
@RequestMapping("/api/gestao/usuarios")      // /api/interno, /api/gestao ou /api/cliente
@PreAuthorize(Papeis.SO_GERENCIAL)           // constantes de seguranca/Papeis
public class XController {

    public record NovoXRequest(@NotBlank(message = "Informe o nome.") String nome) { }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public XDto criar(@Valid @RequestBody NovoXRequest req, JwtAuthenticationToken auth) {
        return servico.criar(..., autor(auth));
    }
}
```

- Request como `record` aninhado com Bean Validation e mensagens em português.
- Controller só traduz: request → comando, token → autor/escopo. Nenhuma regra de negócio.
- Se o papel muda o escopo, use `UsuarioAtual.temPapel(auth, Papeis.X)`.

## 4. Testes

1. **Domínio** (`.../<contexto>/entidade/XTest` e `.../<contexto>/servico/<RegraPura>Test`): entidades, objetos de valor e serviços de domínio testados direto, sem Spring e sem mocks.
2. **Unitário do serviço de aplicação** (`src/test/.../<modulo>/XServiceTest`): portas e repositórios com `mock(...)`, `@DisplayName` em português com a regra. Um teste por critério de aceite e por erro previsto (verifique `getTipo()` e `getCodigo()`).
3. **Segurança** em `SegurancaEndpointsTest`: inclua o controller em `@WebMvcTest(controllers = ...)`, `@MockitoBean` do serviço, e cubra 401 sem token, 403 para cada papel sem acesso e 200/201 para quem tem. Para erro de negócio, confira `$.codigo` e o status.
4. `cd backend && mvn -o -q test`.

## 5. Contrato com o front

Atualize/crie as interfaces em `frontend/src/app/core/api/<nome>-api.service.ts` espelhando os DTOs (mesmos nomes de campo).
