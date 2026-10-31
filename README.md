# CRM Super Auto (Regional Formiga)

CRM de pós-venda para associados de proteção veicular, em migração de uma planilha Excel com VBA para uma aplicação web.

**Parte 1: base do projeto.** Spring Boot + Angular + Keycloak + Postgres funcionando juntos, com login e controle de acesso por papel. Ainda não há entidades de negócio.

## Stack e versões

| Camada | Tecnologia |
|---|---|
| Backend | Java 21 · Spring Boot **4.1.2** (Spring Security 7, resource server JWT) · Spring Data JPA · Liquibase · Actuator · Maven |
| Frontend | Angular **22.2** (standalone, signals, control flow, zoneless) · Angular Material **22.2** (Material 3) · SCSS · `angular-oauth2-oidc` **22.0.2** (Authorization Code + PKCE) |
| Identidade | Keycloak **26.8.1** |
| Banco | PostgreSQL **16** |
| Infra local | Docker Compose: postgres, keycloak, mailpit, backend e frontend (nginx) |

Usamos as linhas estáveis com suporte em out/2026. O Spring Boot 3.x e o Angular 19 já estavam fora do suporte.

## Pré-requisitos

- **Para subir tudo no Docker:** Docker 24+ com Docker Compose v2. Só isso.
- **Para desenvolver fora do Docker:**
  - JDK 21 e Maven 3.9+.
  - Node.js **22.22.3+** (ou 24.15+), com npm 10+. É exigência do Angular 22.

## Como subir

### Tudo no Docker

```bash
docker compose up --build
```

Na primeira vez o processo demora um pouco, porque baixa as imagens e compila o back e o front. O Keycloak já sobe com o realm `crm` importado, sem passo manual. Quando os serviços ficarem saudáveis, acesse **http://localhost:8081**.

Para zerar tudo, inclusive o banco e o realm:

```bash
docker compose down -v
```

### Modo dev (back e front fora do Docker)

1. Suba só a infraestrutura:
   ```bash
   docker compose up postgres keycloak mailpit
   ```
2. Backend, que roda em http://localhost:8080 com profile `dev`:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
3. Frontend, que roda em http://localhost:4200 e repassa `/api` para `localhost:8080` (ver `proxy.conf.json`):
   ```bash
   cd frontend
   npm install
   npm start
   ```

### Builds e testes

```bash
cd backend  && mvn verify                # testes de segurança (200/401/403 × papel × endpoint) + unitários
cd frontend && npx ng build              # build de produção
cd frontend && npx ng test --watch=false # testes unitários (Vitest)
```

## Usuários de teste

A senha de todos é `123456`, sem troca obrigatória.

| Usuário | Papel | Área após o login |
|---|---|---|
| `gerente` | gerencial | `/painel`, com o grupo GESTÃO no menu |
| `vendedor1` | vendedor | `/painel` |
| `atendente1` | atendente | `/painel` |
| `cliente1` | cliente | `/cliente` (tem o atributo `cpf = 12345678909`) |

O Painel (e o Início do cliente) chama `/api/me` e os três endpoints `ping` com o token do usuário logado e mostra o status de cada um. Por exemplo, `vendedor1` vê 403 em `/api/gestao/ping` e `cliente1` vê 403 em `/api/interno/ping`.

Administração do Keycloak: http://localhost:8180, usuário `admin`, senha `admin`.

## Portas e URLs

| Serviço | URL | Observação |
|---|---|---|
| Front (Docker/nginx) | http://localhost:8081 | `/api` é repassado para o backend |
| Front (dev) | http://localhost:4200 | `ng serve` com proxy |
| API | http://localhost:8080/api | `GET /api/me`, `/api/interno/ping`, `/api/gestao/ping`, `/api/cliente/ping` |
| Health | http://localhost:8080/actuator/health | público |
| Keycloak | http://localhost:8180 | realm `crm`; issuer `http://localhost:8180/realms/crm` |
| Mailpit | http://localhost:8025 | e-mails do Keycloak, como o "esqueci minha senha" |
| Postgres | localhost:5432 | bancos `crm` (aplicação) e `keycloak`; usuário/senha `crm`/`crm` |

As portas podem ser trocadas copiando `.env.example` para `.env`. Se mudar a porta do Keycloak, ajuste também o `issuer` em `frontend/src/environments/environment.ts`.

## Como funciona a segurança

- **Keycloak**
  - O client `crm-web` é público, usa Standard Flow com PKCE S256 e é o que o navegador usa.
  - Um *audience mapper* coloca `aud: crm-api` no access token.
  - Um *attribute mapper* expõe o claim `cpf`.
- **Backend**
  - Valida assinatura, expiração, `iss` (`KEYCLOAK_ISSUER`) e `aud` (`crm-api`).
  - Transforma `realm_access.roles` em `ROLE_GERENCIAL`, `ROLE_VENDEDOR`, `ROLE_ATENDENTE` e `ROLE_CLIENTE`. Papéis padrão do Keycloak são ignorados.
  - Protege os endpoints com `@PreAuthorize`. Todo erro sai em ProblemDetail (`application/problem+json`), em português.
- **Issuer e chaves separados.**
  - O `iss` do token é sempre `http://localhost:8180/realms/crm`, que é a URL usada pelo navegador.
  - No Docker, o backend busca as chaves (`KEYCLOAK_JWK_SET_URI`) pela rede interna, em `keycloak:8080`. Por isso não é preciso mexer no `/etc/hosts`.
- **Frontend**
  - O token vai só nas chamadas a `/api`.
  - A renovação usa o refresh token quando 75% da validade passou.
  - O guard de rota e a diretiva `*temPapel="['gerencial']"` só controlam a interface. Quem protege os dados é a API.

### Variáveis de ambiente do backend

| Variável | Padrão (profile `dev`) |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/crm` |
| `DB_USER` / `DB_PASSWORD` | `crm` / `crm` |
| `KEYCLOAK_ISSUER` | `http://localhost:8180/realms/crm` |
| `KEYCLOAK_JWK_SET_URI` | `http://localhost:8180/realms/crm/protocol/openid-connect/certs` |
| `KEYCLOAK_AUDIENCIA` | `crm-api` |
| `CORS_ORIGENS` | `http://localhost:4200` (lista separada por vírgula) |

O fuso padrão é `America/Sao_Paulo`, exposto como bean `java.time.Clock`. Regras de data devem injetar esse `Clock` em vez de usar `LocalDate.now()`.

## Exportar o realm depois de alterar algo no Keycloak

O import (`--import-realm`) só acontece quando o realm ainda não existe no banco. Depois de mudar algo pelo console de administração, gere um novo `realm-export.json` para versionar:

```bash
# 1. Exporta realm + usuários para infra/keycloak/crm-realm.json (roda um Keycloak avulso no mesmo banco)
docker compose run --rm --no-deps \
  -v "$(pwd)/infra/keycloak:/opt/keycloak/data/export" \
  keycloak export --dir /opt/keycloak/data/export --realm crm --users realm_file

# 2. Substitui o arquivo versionado
mv infra/keycloak/crm-realm.json infra/keycloak/realm-export.json

# 3. Para testar o import do zero
docker compose down -v && docker compose up --build
```

Dois cuidados:
- A exportação traz as senhas como **hash**, não mais `123456` em texto, e inclui o *secret* gerado para o client `crm-api`. Para um ambiente local isso funciona igual, mas revise o diff antes de commitar.
- No PowerShell, troque `$(pwd)` por `${PWD}`.

## Estrutura

```
crm-superauto/
├── docker-compose.yml, .env.example
├── infra/
│   ├── postgres/init/01-criar-bancos.sql    # cria o banco do Keycloak
│   └── keycloak/realm-export.json           # realm crm versionado
├── backend/   (br.com.superauto.crm)
│   ├── config/      SecurityConfig, ClockConfig, CrmProperties
│   ├── seguranca/   Papeis, KeycloakRealmRoleConverter, AudienceValidator, DelegarParaExceptionHandler
│   ├── erro/        GlobalExceptionHandler (ProblemDetail em pt-BR)
│   ├── api/         MeController, PingController, dto/
│   └── resources/db/changelog/   # Liquibase: db.changelog-master.xml + changes/ (um arquivo por área) + dados/
└── frontend/src/app/
    ├── core/auth/   AuthService (signals), interceptor, guards, *temPapel
    ├── core/api/    ApiService
    ├── layout/      interno (menu lateral) e cliente (navegação inferior)
    ├── paginas/     painel, cliente-inicio, em-construcao, acesso-negado
    └── shared/      status-chip, verificacao-integracao
```

**Convenção de nomes:**
- O domínio fica em português (pacotes `seguranca`, `erro`, `api`; classes como `Papeis` e `LayoutInterno`).
- Termos técnicos do framework ficam como o framework chama (`config`, `SecurityConfig`, `Controller`, `Dto`).
- No Angular seguimos o guia de estilo atual: arquivos sem o sufixo `.component` e classes sem o sufixo `Component`.
