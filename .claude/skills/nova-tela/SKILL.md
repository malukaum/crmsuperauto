---
name: nova-tela
description: Cria ou altera uma página do frontend Angular do CRM — cliente da API, componente standalone com signals, rota protegida por papel, item de menu, formatos brasileiros e testes Vitest. Use ao construir telas, diálogos ou substituir uma rota "em construção".
---

# Nova tela (Angular 22)

Use `frontend/src/app/paginas/acesso/usuarios-equipe/` como referência viva de padrão.

**Obrigatório:** DDD, SOLID e Clean Code também no frontend (seção "Princípios obrigatórios" do `CLAUDE.md`).
Componente só cuida da apresentação e do estado da tela; chamadas HTTP ficam no serviço de `core/api`;
regras reutilizáveis em funções puras exportadas (ex.: `nomeCompleto`, `problemaDe`); diálogos e
componentes pequenos em arquivos próprios; nomes que revelam intenção; sem textos/números mágicos.
**DDD no front:** a pasta segue o contexto do backend (`paginas/<contexto>/<tela>`, ex.: `paginas/acesso/usuarios-equipe`)
e nomes de arquivos, componentes, campos e rótulos usam a linguagem ubíqua das histórias.

## 1. Cliente da API — `core/api/<nome>-api.service.ts`

```ts
@Injectable({ providedIn: 'root' })
export class XApi {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiBase}/interno/x`;

  listar(): Observable<X[]> {
    return this.http.get<X[]>(this.base);
  }
}
```

- Interfaces TypeScript espelham os DTOs do backend, com os mesmos nomes de campo; Javadoc curto nos campos não óbvios.
- Erros: a API devolve ProblemDetail (`Problema` em `api.service.ts`); estenda com `codigo` e os extras do caso de uso.

## 2. Componente — `paginas/<contexto>/<nome>/<nome>.ts|html|scss`

- `standalone` (padrão), `changeDetection: ChangeDetectionStrategy.OnPush`, `inject()` para dependências.
- Estado em `signal` (`carregando`, `erro`, dados) e derivados em `computed`. Sem `BehaviorSubject` para estado de tela.
- Template com `@if` / `@for (...; track ...)`; nada de `*ngIf`/`*ngFor`.
- Angular Material 3 para componentes; tokens de `src/styles/_tokens.scss` e `_theme-colors.scss` para cores e espaçamentos.
- Diálogos em arquivos próprios na mesma pasta (`novo-x-dialog.ts`).
- Mensagens de erro: mostre `problema.detail`; use `problema.codigo` para decidir fluxos (ex.: `contratos-a-transferir` abre outro diálogo).
- Comentário de uma linha no topo: `/** CAR-01 · Buscar e filtrar a carteira. */`.

## 3. Regras de exibição

- RG-02: datas `dd/mm/aaaa`, moeda `R$ 1.234,56`, telefone `(37) 9 9999-9999`, placa maiúscula sem hífen (pipes com locale `pt-BR`).
- RG-04: CPF/CNPJ mascarado para atendente.
- RG-09: toda tela com mensalidade mostra o aviso de que são valores contratados, não recebidos.
- RG-10: WhatsApp só abre a conversa (`https://wa.me/...`), nunca envia.
- Esconda ações sem permissão com a diretiva `temPapel` — mas a segurança real está no backend.

## 4. Rota e menu

- `app.routes.ts`: `loadComponent`, `title: 'Nome' + SUFIXO`, `canActivate: [papelGuard]`, `data: { papeis: [...] satisfies Papel[] }`. Substitua o `emConstrucao(...)` correspondente se existir.
- Item de menu em `layout/interno/layout-interno.ts|html`, visível pelos mesmos papéis.

## 5. Verificar

```bash
cd frontend && npx ng build
cd frontend && npx ng test --watch=false
```

Teste (`*.spec.ts`, Vitest) pelo menos o cliente da API e as regras de tela não triviais. Se o usuário quiser ver funcionando, suba com `npm start` (proxy para `localhost:8080`) e use os usuários de teste do README (`gerente`, `vendedor1`, `atendente1`, senha `123456`).
