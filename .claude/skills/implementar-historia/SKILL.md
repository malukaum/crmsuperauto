---
name: implementar-historia
description: Implementa uma história de usuário do CRM (ACE-xx, CAR-xx, FIL-xx, ATD-xx, IND-xx, INT-xx, IMP-xx, CLI-xx, AUD-xx) de ponta a ponta — banco, backend, frontend, testes e documentação. Use quando o pedido citar o id de uma história ou uma funcionalidade descrita em docs/historias.
---

# Implementar uma história

Siga as etapas em ordem. Não pule a leitura: as regras estão nos documentos, não no código.

## 1. Entender

1. Abra `docs/historias/README.md`, ache a história no mapa e abra o arquivo do módulo.
2. Leia a história inteira: *Como/quero/para*, **Regras** e **Critérios de aceite**.
3. Leia as **regras gerais RG-01…RG-14** que ela toca (no mínimo: RG-01 hoje em SP, RG-03 escopo por papel, RG-06 edição simultânea, RG-07 nada se apaga, RG-08 auditoria).
4. Regras marcadas **(proposta)** ainda não foram validadas pelo negócio: implemente, mas avise o usuário no resumo final.
5. Anote os **termos do negócio** usados na história: eles serão os nomes de classes, métodos, campos e telas (linguagem ubíqua). Termo ambíguo → pergunte.
6. Consulte `docs/modelo-de-dados/dicionario-de-dados.md` para as tabelas envolvidas e confira o que já está mapeado (`grep -rn "@Table" backend/src/main/java`).

## 2. Planejar

Liste antes de codar (e mostre ao usuário se a história for grande ou ambígua):
- **Contexto delimitado** dono da história (tabela de contextos no `CLAUDE.md`) e se ela toca outros contextos (como eles vão se comunicar: porta ou evento de domínio).
- **Agregados** envolvidos, a raiz de cada um, as invariantes que a raiz protege e os **objetos de valor** necessários.
- Quais operações são **escrita** (passam pelo agregado) e quais são **leitura** (projeção/visão).
- Tabelas/visões a mapear ou criar → skill `nova-entidade`.
- Casos de uso e endpoints, com papel de cada um → skill `novo-endpoint`.
- Telas e rotas → skill `nova-tela`.
- Cada critério de aceite → em que teste ele vira.

Se um critério for contraditório ou depender de decisão de negócio, pergunte em vez de inventar.

## 3. Implementar

Na ordem: banco → `entidade` (agregados, objetos de valor, eventos) → `repositorio` → `servico` (primeiro as regras puras do domínio, testadas sem Spring; depois os casos de uso) → `infraestrutura` → `api` → frontend.

**O projeto segue DDD, a arquitetura é SOLID e o código segue Clean Code** — releia a seção "Princípios obrigatórios" do `CLAUDE.md` antes de começar. Além disso: Hibernate sem SQL nativo, `NegocioException` com `Tipo`, auditoria e escopo por papel. Funcionar não basta: código que fere DDD, SOLID ou Clean Code deve ser refeito antes de concluir.

## 4. Verificar

- `cd backend && mvn -o -q test`
- Mapeamento JPA novo/alterado → skill `validar-banco`.
- Frontend alterado → `cd frontend && npx ng build && npx ng test --watch=false`.
- Rode a skill `revisar-convencoes`.

## 5. Documentar e reportar

- Se a implementação exigiu decidir algo que a história não dizia, registre a decisão na própria história.
- Resumo final: o que foi feito por critério de aceite (atendido / parcialmente / não), testes rodados com resultado real, regras **(proposta)** envolvidas e pendências.
