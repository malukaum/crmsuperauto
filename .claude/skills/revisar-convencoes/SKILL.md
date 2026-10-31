---
name: revisar-convencoes
description: Checklist das convenções do CRM (DDD, arquitetura SOLID, Clean Code, Hibernate sem SQL nativo, regras gerais RG, segurança, testes, documentação) aplicado às mudanças pendentes. Use antes de commit ou PR, ou quando pedirem para revisar se o código segue o padrão do projeto.
---

# Revisar convenções nas mudanças

Olhe só o que mudou:

```bash
git status --short && git diff --stat
```

## Varreduras automáticas

```bash
cd backend
# SQL nativo no código Java (deve vir vazio)
grep -rn "nativeQuery\|JdbcTemplate\|NamedParameterJdbc\|createNativeQuery" src/main/java
# entidade/repositorio/servico importando web/infra (deve vir vazio)
grep -rln "org.springframework.http\|oauth2.jwt\|RestClient\|jakarta.servlet\|web.bind" \
  src/main/java/br/com/superauto/crm/*/{entidade,repositorio,servico}
# Camada interna dependendo de camada externa (deve vir vazio):
# entidade não importa repositorio/servico/infraestrutura/api; repositorio não importa servico/infraestrutura/api
grep -rn "import br.com.superauto.crm\.[a-z]*\.\(repositorio\|servico\|infraestrutura\|api\)\." \
  src/main/java/br/com/superauto/crm/*/entidade
grep -rn "import br.com.superauto.crm\.[a-z]*\.\(servico\|infraestrutura\|api\)\." \
  src/main/java/br/com/superauto/crm/*/repositorio
# servico não importa infraestrutura/api (deve vir vazio)
grep -rn "import br.com.superauto.crm\.[a-z]*\.\(infraestrutura\|api\)\." src/main/java/br/com/superauto/crm/*/servico
# Serviço de domínio (regra pura) não pode ter Spring: confira as classes de servico/ sem @Service
grep -L "@Service\|@Component" src/main/java/br/com/superauto/crm/*/servico/*.java \
  | xargs -r grep -ln "import org.springframework"
# Repositório de outro contexto usado fora de infraestrutura (deve vir vazio)
for ctx in $(ls src/main/java/br/com/superauto/crm); do
  grep -rn "import br.com.superauto.crm\.[a-z]*\.repositorio\.[A-Za-z]*Repository;" \
    src/main/java/br/com/superauto/crm/$ctx/servico src/main/java/br/com/superauto/crm/$ctx/api \
    2>/dev/null | grep -v "crm\.$ctx\.repositorio"
done
# Classes soltas na raiz de um contexto (deveriam estar em entidade/repositorio/servico/infraestrutura/api)
find src/main/java/br/com/superauto/crm/*/ -maxdepth 1 -name "*.java" | grep -v "/\(erro\|seguranca\|config\)/"
# @ManyToOne: revisar cada ocorrência (só vale dentro do mesmo agregado)
grep -rn "@ManyToOne" src/main/java
# Data "de hoje" sem Clock
grep -rn "LocalDate.now()\|OffsetDateTime.now()\|Instant.now()" src/main/java
# Endpoint sem @PreAuthorize: confira cada controller alterado
```

## Checklist manual

O projeto segue **DDD**, a arquitetura é **SOLID** e o código deve seguir **Clean Code** (seção "Princípios obrigatórios" do `CLAUDE.md`).
Falha nestes itens bloqueia o commit tanto quanto teste vermelho.

**DDD**
- [ ] Nomes de classes, métodos, campos, endpoints e telas usam a linguagem ubíqua das histórias.
- [ ] Classe no contexto e na camada certos (`entidade`, `repositorio`, `servico`, `infraestrutura`, `api`); dependências só apontam para dentro (`api → servico → repositorio → entidade`).
- [ ] Em `servico/`, regra pura (serviço de domínio) sem Spring; caso de uso (serviço de aplicação) com `@Service` e sem regra de negócio.
- [ ] Regra de negócio na entidade, objeto de valor ou serviço de domínio; serviço de aplicação só orquestra.
- [ ] Agregado alterado só pela raiz; repositório só para raiz; partes internas sem repositório.
- [ ] Entre agregados, referência por id (sem `@ManyToOne` para outro agregado).
- [ ] Conceitos com regra de formato como objetos de valor (placa, telefone, documento, e-mail, mensalidade), não primitivos.
- [ ] Comunicação entre contextos por porta (definida pelo consumidor, implementada pelo dono do dado) ou evento de domínio; nunca usando entidade/repositório do outro contexto.
- [ ] Sistema externo atrás de camada anticorrupção; o modelo externo não vaza para o domínio.
- [ ] Domínio testado sem Spring e sem mocks.

**SOLID**
- [ ] **S:** cada classe alterada tem um único motivo para mudar (sinal de alerta: >200 linhas, métodos `private` de naturezas diferentes, nome com "E"/"Gerenciador").
- [ ] **O:** comportamento novo não espalhou `if`/`switch` sobre o mesmo tipo em várias classes.
- [ ] **L:** implementações de porta respeitam o contrato da interface (retornos e exceções documentados).
- [ ] **I:** interfaces novas só têm o que o cliente usa; repositório de leitura não expõe `save`/`delete`.
- [ ] **D:** serviço depende de interfaces/repositórios, não de clientes concretos; controller sem regra de negócio; domínio sem `Jwt`/`HttpStatus`/`RestClient`.

**Clean Code**
- [ ] Nomes revelam intenção no vocabulário do negócio; nada de `aux`, `tmp`, `dados2`, `Utils`, `Helper`.
- [ ] Métodos curtos (~20 linhas), fazendo uma coisa, num nível de abstração; até 3 parâmetros (senão, `record`).
- [ ] Sem números/textos mágicos: constantes ou enums.
- [ ] Sem duplicação, código morto ou comentado.
- [ ] Comentários explicam o porquê; nenhum comentário descrevendo o que o código já diz.
- [ ] Busca devolve `Optional`; falha cedo com exceção significativa; nenhuma exceção engolida sem log e motivo.
- [ ] DTOs e valores como `record`; campos `final`; entidades com métodos de domínio em vez de setters.
- [ ] Testes com um conceito cada, `@DisplayName` descrevendo a regra.

**Persistência**
- [ ] Entidade com construtor `protected`, métodos de domínio em vez de setters, `@Version` onde há `versao`.
- [ ] Changeset novo (nenhum já aplicado foi editado), com rollback; dicionário de dados atualizado.
- [ ] Mapeamento novo validado com a skill `validar-banco`.

**Regras gerais**
- [ ] RG-03: consultas novas filtram pelo vendedor quando o papel é vendedor.
- [ ] RG-07: nada é excluído (contrato/atendimento/usuário).
- [ ] RG-08: alterações auditadas com anterior e novo.
- [ ] RG-01: "hoje" pelo `Clock` / fuso de São Paulo.

**API e erros**
- [ ] `@PreAuthorize` com constantes de `Papeis`; endpoint no `SegurancaEndpointsTest`.
- [ ] `NegocioException` com `Tipo` e `codigo` kebab-case; mensagens em português para o usuário final.

**Frontend**
- [ ] OnPush, signals, `inject()`, control flow novo; rota com `papelGuard`; formatos RG-02.

**Testes e docs**
- [ ] Critérios de aceite cobertos por testes com `@DisplayName` em português.
- [ ] `mvn -o -q test` (e `npx ng build` / `npx ng test --watch=false` se o front mudou) verdes — rodados de fato.
- [ ] História em `docs/historias` atualizada se a regra mudou.

Relate os itens que falharam com `arquivo:linha` e a correção sugerida; corrija se o usuário pedir.
