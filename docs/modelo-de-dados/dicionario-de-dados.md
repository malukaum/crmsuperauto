# Dicionário de dados

> Gerado a partir do banco (catálogo do PostgreSQL e comentários da migration `V2__modelo_dominio.sql`) pelo script [`scripts/db/gerar_dicionario.sql`](../../scripts/db/gerar_dicionario.sql). Não edite à mão: altere a migration e gere de novo.

Visão geral, decisões e regras: [Modelo de dados](README.md).

## Tabelas

### `unidade_federativa`

**Módulo:** Referência. As 27 unidades federativas do Brasil.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `sigla` | `character(2)` | não |  | PK |
| `nome` | `character varying(40)` | não |  |  |

**Regras de integridade**

- `ck_unidade_federativa_sigla`: `CHECK ((sigla ~ '^[A-Z]{2}$'))`

### `municipio`

**Módulo:** Referência. Municípios do IBGE. Carga completa em migration própria.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `integer` | não | identity | PK |
| `codigo_ibge` | `character(7)` | não |  | UK |
| `nome` | `character varying(80)` | não |  |  |
| `uf_sigla` | `character(2)` | não |  | FK → `unidade_federativa` |

**Regras de integridade**

- `uk_municipio_nome_uf`: `UNIQUE (nome, uf_sigla)`
- `ck_municipio_codigo_ibge`: `CHECK ((codigo_ibge ~ '^[0-9]{7}$'))`

### `tipo_veiculo`

**Módulo:** Referência. Classificação do veículo protegido. Editável pela equipe; desativar em vez de apagar.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `smallint` | não | identity | PK |
| `codigo` | `character varying(30)` | não |  | UK |
| `descricao` | `character varying(80)` | não |  |  |
| `ativo` | `boolean` | não | true |  |

### `tipo_contato`

**Módulo:** Referência. Natureza do atendimento (pós-venda, aniversário, suporte...). Só POS_VENDA move a régua.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `smallint` | não | identity | PK |
| `codigo` | `character varying(30)` | não |  | UK |
| `descricao` | `character varying(80)` | não |  |  |
| `ativo` | `boolean` | não | true |  |

### `etapa_relacionamento`

**Módulo:** Referência. Régua de pós-venda D+7 → D+30 → D+60 → D+90 → Trimestral. Substitui a aba CONFIGURACOES.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `smallint` | não | identity | PK |
| `codigo` | `character varying(20)` | não |  | UK |
| `nome` | `character varying(40)` | não |  |  |
| `ordem` | `smallint` | não |  | UK |
| `dias_apos_anterior` | `smallint` | não |  | Dias após o contato anterior concluído (ou após a adesão, na etapa inicial) até este contato. |
| `proxima_etapa_id` | `smallint` | sim |  | FK → `etapa_relacionamento`; Etapa que fica pendente quando esta é concluída. Trimestral aponta para si mesma. |
| `inicial` | `boolean` | não | false | Etapa em que todo contrato novo entra (só uma). |

**Regras de integridade**

- `ck_etapa_relacionamento_dias`: `CHECK (((dias_apos_anterior >= 1) AND (dias_apos_anterior <= 365)))`

**Índices adicionais**

- `uk_etapa_relacionamento_inicial`: `UNIQUE btree (inicial) WHERE inicial`

### `usuario`

**Módulo:** Acesso. Projeção local de quem acessa pelo Keycloak (equipe e clientes). Papéis ficam só no Keycloak.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `uuid` | não |  | PK; Claim "sub" do token do Keycloak. |
| `username` | `character varying(100)` | não |  | UK |
| `nome` | `character varying(200)` | não |  |  |
| `email` | `character varying(254)` | sim |  |  |
| `ativo` | `boolean` | não | true | Falso para desativados e para o usuário técnico sistema.importacao. |
| `criado_em` | `timestamp with time zone` | não | now() |  |
| `atualizado_em` | `timestamp with time zone` | não | now() |  |

### `convite_acesso`

**Módulo:** Acesso. Códigos de primeiro acesso à área do cliente.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | não | identity | PK |
| `associado_id` | `bigint` | não |  | FK → `associado` |
| `contato_id` | `bigint` | não |  | FK → `contato_associado` |
| `codigo_hash` | `character varying(100)` | não |  | Hash do código enviado; o código nunca é gravado em claro. |
| `expira_em` | `timestamp with time zone` | não |  |  |
| `usado_em` | `timestamp with time zone` | sim |  |  |
| `tentativas` | `smallint` | não | 0 |  |
| `criado_em` | `timestamp with time zone` | não | now() |  |
| `criado_por` | `uuid` | sim |  | FK → `usuario` |

**Regras de integridade**

- `ck_convite_tentativas`: `CHECK (((tentativas >= 0) AND (tentativas <= 10)))`
- `ck_convite_validade`: `CHECK ((expira_em > criado_em))`

**Índices adicionais**

- `ix_convite_associado`: `btree (associado_id) WHERE (usado_em IS NULL)`

### `associado`

**Módulo:** Cadastro. Pessoa física ou jurídica associada. Uma por documento, com quantos contratos tiver.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | não | identity | PK |
| `nome` | `character varying(200)` | não |  |  |
| `documento` | `character varying(14)` | sim |  | UK; CPF (11) ou CNPJ (14, aceita alfanumérico), sem pontuação. Vazio apenas após anonimização. |
| `data_nascimento` | `date` | sim |  | Só para pessoa física. Alimenta os aniversariantes. |
| `municipio_id` | `integer` | sim |  | FK → `municipio` |
| `perfil` | `character varying(20)` | não | 'REGULAR' | REGULAR, PROMOTOR ou ATENCAO. Uso interno; o cliente não vê. |
| `potencial_indicacao` | `character varying(20)` | não | 'NAO_AVALIADO' | NAO_AVALIADO, ALTO, MEDIO ou BAIXO. Uso interno. |
| `usuario_id` | `uuid` | sim |  | UK, FK → `usuario`; Conta do associado na área do cliente, criada no primeiro acesso. |
| `anonimizado_em` | `timestamp with time zone` | sim |  | Preenchido quando os dados pessoais foram apagados a pedido do titular (irreversível). |
| `criado_em` | `timestamp with time zone` | não | now() |  |
| `atualizado_em` | `timestamp with time zone` | não | now() |  |
| `versao` | `integer` | não | 0 |  |

**Regras de integridade**

- `ck_associado_documento`: `CHECK ((((documento) ~ '^[0-9]{11}$') OR ((documento) ~ '^[0-9A-Z]{12}[0-9]{2}$')))`
- `ck_associado_documento_obrigatorio`: `CHECK (((documento IS NOT NULL) OR (anonimizado_em IS NOT NULL)))`
- `ck_associado_nascimento_pf`: `CHECK (((data_nascimento IS NULL) OR (length((documento)) = 11)))`
- `ck_associado_perfil`: `CHECK (((perfil) = ANY (ARRAY['REGULAR', 'PROMOTOR', 'ATENCAO'])))`
- `ck_associado_potencial`: `CHECK (((potencial_indicacao) = ANY (ARRAY['NAO_AVALIADO', 'ALTO', 'MEDIO', 'BAIXO'])))`

**Índices adicionais**

- `ix_associado_aniversario`: `btree (EXTRACT(month FROM data_nascimento), EXTRACT(day FROM data_nascimento)) WHERE (data_nascimento IS NOT NULL)`
- `ix_associado_municipio`: `btree (municipio_id)`
- `ix_associado_nome_busca`: `gin (texto_busca((nome)) gin_trgm_ops)`

### `contato_associado`

**Módulo:** Cadastro. WhatsApp, telefones e e-mails do associado; um principal por tipo.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | não | identity | PK |
| `associado_id` | `bigint` | não |  | FK → `associado` |
| `tipo` | `character varying(10)` | não |  |  |
| `valor` | `character varying(254)` | não |  | Telefone: 55 + DDD + número, só dígitos. E-mail: endereço completo. |
| `principal` | `boolean` | não | false | Contato usado nas filas e no primeiro acesso; um por tipo. |
| `aceita_contato` | `boolean` | não | true | Falso = opt-out: o número não é oferecido para mensagem. |
| `criado_em` | `timestamp with time zone` | não | now() |  |

**Regras de integridade**

- `uk_contato_associado`: `UNIQUE (associado_id, tipo, valor) DEFERRABLE INITIALLY DEFERRED` (conferida no commit, changeset 015)
- `ck_contato_associado_tipo`: `CHECK (((tipo) = ANY (ARRAY['WHATSAPP', 'TELEFONE', 'EMAIL'])))`
- `ck_contato_associado_valor`: `CHECK (((((tipo) = ANY (ARRAY['WHATSAPP', 'TELEFONE'])) AND ((valor) ~ '^55[1-9][0-9]{9,10}$')) OR (((tipo) = 'EMAIL') AND ((valor) ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'))))`

**Índices adicionais**

- `ix_contato_associado_valor`: `btree (valor)`
- `ix_contato_associado_valor_trgm`: `gin (valor gin_trgm_ops)` (busca por trecho do telefone, RG-05)
- `uk_contato_associado_principal`: `EXCLUDE USING btree (associado_id WITH =, tipo WITH =) WHERE (principal) DEFERRABLE INITIALLY DEFERRED` (um principal por tipo, conferido no commit: trocar o principal mexe em duas linhas na mesma gravação)

### `veiculo`

**Módulo:** Cadastro. Veículo identificado pela placa, independente de quem o protege.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | não | identity | PK |
| `placa` | `character varying(7)` | não |  | UK; Maiúsculas, sem hífen; padrão antigo ou Mercosul. |
| `modelo` | `character varying(150)` | não |  |  |
| `tipo_veiculo_id` | `smallint` | não |  | FK → `tipo_veiculo` |
| `criado_em` | `timestamp with time zone` | não | now() |  |
| `atualizado_em` | `timestamp with time zone` | não | now() |  |
| `versao` | `integer` | não | 0 |  |

**Regras de integridade**

- `ck_veiculo_placa`: `CHECK (((placa) ~ '^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$'))`

### `contrato`

**Módulo:** Cadastro. Proteção de um veículo para um associado. Espelha os dados do SGA.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | não | identity | PK |
| `associado_id` | `bigint` | não |  | FK → `associado` |
| `veiculo_id` | `bigint` | não |  | FK → `veiculo` |
| `vendedor_id` | `uuid` | sim |  | FK → `usuario`; Vendedor dono do contrato; define o que o papel vendedor enxerga. |
| `codigo_sga` | `character varying(30)` | sim |  | UK |
| `situacao` | `character varying(12)` | não | 'A_CONFERIR' | A_CONFERIR, ATIVO, SUSPENSO ou CANCELADO. Suspensos e cancelados ficam fora das filas. |
| `valor_mensalidade` | `numeric(12,2)` | não |  | Valor contratado atual. Não é receita recebida; reajustes ficam na auditoria. |
| `vendido_em` | `timestamp with time zone` | sim |  | Data da venda (DATA VENDA da base histórica). |
| `aderido_em` | `timestamp with time zone` | não |  | Cadastro no SGA; base do primeiro contato da régua. |
| `encerrado_em` | `timestamp with time zone` | sim |  | Data do cancelamento; obrigatória quando CANCELADO. |
| `origem` | `character varying(12)` | não |  | MANUAL, IMPORTACAO ou SGA. |
| `importacao_registro_id` | `bigint` | sim |  | FK → `importacao_registro` |
| `criado_em` | `timestamp with time zone` | não | now() |  |
| `atualizado_em` | `timestamp with time zone` | não | now() |  |
| `versao` | `integer` | não | 0 |  |

**Regras de integridade**

- `uk_contrato_id_associado`: `UNIQUE (id, associado_id)`
- `ck_contrato_encerramento`: `CHECK ((((situacao) = 'CANCELADO') = (encerrado_em IS NOT NULL)))`
- `ck_contrato_mensalidade`: `CHECK ((valor_mensalidade >= (0)::numeric))`
- `ck_contrato_origem`: `CHECK (((origem) = ANY (ARRAY['MANUAL', 'IMPORTACAO', 'SGA'])))`
- `ck_contrato_origem_importacao`: `CHECK ((((origem) = 'IMPORTACAO') = (importacao_registro_id IS NOT NULL)))`
- `ck_contrato_situacao`: `CHECK (((situacao) = ANY (ARRAY['A_CONFERIR', 'ATIVO', 'SUSPENSO', 'CANCELADO'])))`

**Índices adicionais**

- `ix_contrato_associado`: `btree (associado_id)`
- `ix_contrato_situacao`: `btree (situacao)`
- `ix_contrato_vendedor`: `btree (vendedor_id)`
- `uk_contrato_veiculo_vigente`: `UNIQUE btree (veiculo_id) WHERE ((situacao) <> 'CANCELADO')`

### `acompanhamento_contrato`

**Módulo:** Relacionamento. Estado da régua de pós-venda de cada contrato (1:1).

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `contrato_id` | `bigint` | não |  | PK, FK → `contrato` |
| `etapa_pendente_id` | `smallint` | sim |  | FK → `etapa_relacionamento`; Próximo contato da régua a realizar. Vazio = régua encerrada. |
| `retorno_manual_em` | `date` | sim |  | Data combinada ou de nova tentativa; prevalece sobre o cálculo. |
| `proximo_contato_em` | `date` | sim |  | DERIVADO e gravado para indexar as filas: retorno manual, ou último contato concluído + prazo da etapa pendente, ou adesão + prazo da etapa inicial. |
| `encerrado_em` | `timestamp with time zone` | sim |  |  |
| `atualizado_em` | `timestamp with time zone` | não | now() |  |
| `versao` | `integer` | não | 0 |  |

**Regras de integridade**

- `ck_acompanhamento_encerrado`: `CHECK (((etapa_pendente_id IS NULL) = (encerrado_em IS NOT NULL)))`

**Índices adicionais**

- `ix_acompanhamento_proximo_contato`: `btree (proximo_contato_em) WHERE (etapa_pendente_id IS NOT NULL)`

### `atendimento`

**Módulo:** Relacionamento. Histórico imutável de contatos. Correção = novo registro. Substitui a aba ATENDIMENTOS.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | não | identity | PK |
| `associado_id` | `bigint` | não |  | FK → `associado` |
| `contrato_id` | `bigint` | sim |  | Vazio em contatos sobre a pessoa, como mensagem de aniversário. |
| `tipo_contato_id` | `smallint` | não |  | FK → `tipo_contato` |
| `etapa_id` | `smallint` | sim |  | FK → `etapa_relacionamento`; Etapa da régua trabalhada neste contato. |
| `resultado` | `character varying(15)` | não |  | CONCLUIDO, PENDENTE, SEM_RESPOSTA ou REAGENDADO. |
| `descricao` | `character varying(2000)` | sim |  |  |
| `retorno_agendado_para` | `date` | sim |  | Data de retorno informada (DATA EXCEÇÃO na planilha). Obrigatória em REAGENDADO. |
| `corrige_atendimento_id` | `bigint` | sim |  | FK → `atendimento`; Atendimento que este registro corrige. |
| `realizado_em` | `timestamp with time zone` | não |  | Quando o contato aconteceu. |
| `registrado_em` | `timestamp with time zone` | não | now() | Quando foi lançado no sistema. |
| `registrado_por` | `uuid` | não |  | FK → `usuario` |

**Regras de integridade**

- `fk_atendimento_contrato`: `FOREIGN KEY (contrato_id, associado_id) REFERENCES contrato(id, associado_id)`
- `ck_atendimento_datas`: `CHECK ((realizado_em <= (registrado_em + '00:05:00')))`
- `ck_atendimento_etapa_contrato`: `CHECK (((etapa_id IS NULL) OR (contrato_id IS NOT NULL)))`
- `ck_atendimento_reagendado`: `CHECK ((((resultado) <> 'REAGENDADO') OR (retorno_agendado_para IS NOT NULL)))`
- `ck_atendimento_resultado`: `CHECK (((resultado) = ANY (ARRAY['CONCLUIDO', 'PENDENTE', 'SEM_RESPOSTA', 'REAGENDADO'])))`

**Índices adicionais**

- `ix_atendimento_associado`: `btree (associado_id, realizado_em DESC)`
- `ix_atendimento_contrato_concluido`: `btree (contrato_id, realizado_em DESC) WHERE (((resultado) = 'CONCLUIDO') AND (etapa_id IS NOT NULL))`
- `ix_atendimento_realizado_em`: `btree (realizado_em)`
- `ix_atendimento_usuario`: `btree (registrado_por)`

### `indicacao`

**Módulo:** Comercial. Indicações feitas por associados, pela equipe ou pela área do cliente.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | não | identity | PK |
| `indicador_id` | `bigint` | não |  | FK → `associado` |
| `indicado_nome` | `character varying(200)` | não |  |  |
| `indicado_telefone` | `character varying(13)` | não |  |  |
| `veiculo_interesse` | `character varying(150)` | sim |  |  |
| `canal` | `character varying(12)` | não |  | EQUIPE ou AREA_CLIENTE. |
| `status` | `character varying(14)` | não | 'RECEBIDA' | RECEBIDA, EM_NEGOCIACAO, CONVERTIDA ou PERDIDA. |
| `consentimento_declarado` | `boolean` | não |  | Declaração de que o indicado autorizou o contato; obrigatória. |
| `contrato_convertido_id` | `bigint` | sim |  | UK, FK → `contrato`; Contrato fechado com o indicado (PLACA FECHADA na planilha). |
| `observacao` | `character varying(1000)` | sim |  |  |
| `registrada_em` | `timestamp with time zone` | não | now() |  |
| `registrada_por` | `uuid` | sim |  | FK → `usuario` |
| `atualizado_em` | `timestamp with time zone` | não | now() |  |
| `versao` | `integer` | não | 0 |  |

**Regras de integridade**

- `ck_indicacao_canal`: `CHECK (((canal) = ANY (ARRAY['EQUIPE', 'AREA_CLIENTE'])))`
- `ck_indicacao_consentimento`: `CHECK (consentimento_declarado)`
- `ck_indicacao_convertida`: `CHECK ((((status) = 'CONVERTIDA') = (contrato_convertido_id IS NOT NULL)))`
- `ck_indicacao_registro_equipe`: `CHECK ((((canal) <> 'EQUIPE') OR (registrada_por IS NOT NULL)))`
- `ck_indicacao_status`: `CHECK (((status) = ANY (ARRAY['RECEBIDA', 'EM_NEGOCIACAO', 'CONVERTIDA', 'PERDIDA'])))`
- `ck_indicacao_telefone`: `CHECK (((indicado_telefone) ~ '^55[1-9][0-9]{9,10}$'))`

**Índices adicionais**

- `ix_indicacao_indicador`: `btree (indicador_id)`
- `ix_indicacao_status`: `btree (status)`
- `ix_indicacao_telefone`: `btree (indicado_telefone)`

### `importacao`

**Módulo:** Importação. Cada arquivo de planilha importado.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | não | identity | PK |
| `arquivo_nome` | `character varying(255)` | não |  |  |
| `arquivo_sha256` | `character(64)` | não |  | UK; Impede importar o mesmo arquivo duas vezes. |
| `status` | `character varying(12)` | não | 'PROCESSANDO' |  |
| `iniciada_em` | `timestamp with time zone` | não | now() |  |
| `concluida_em` | `timestamp with time zone` | sim |  |  |
| `importada_por` | `uuid` | não |  | FK → `usuario` |

**Regras de integridade**

- `ck_importacao_conclusao`: `CHECK ((((status) = 'PROCESSANDO') = (concluida_em IS NULL)))`
- `ck_importacao_status`: `CHECK (((status) = ANY (ARRAY['PROCESSANDO', 'CONCLUIDA', 'FALHOU'])))`

### `importacao_registro`

**Módulo:** Importação. Cada linha lida da planilha, crua, com o resultado. Substitui a aba BASE HISTORICA.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | não | identity | PK |
| `importacao_id` | `bigint` | não |  | FK → `importacao` |
| `aba` | `character varying(40)` | não |  |  |
| `linha` | `integer` | não |  |  |
| `dados_originais` | `jsonb` | não |  | Linha da planilha exatamente como veio, em JSON. |
| `situacao` | `character varying(20)` | não |  | IMPORTADO, DUPLICADO ou REJEITADO. |
| `mensagem` | `character varying(500)` | sim |  |  |

**Regras de integridade**

- `uk_importacao_registro_linha`: `UNIQUE (importacao_id, aba, linha)`
- `ck_importacao_registro_linha`: `CHECK ((linha > 0))`
- `ck_importacao_registro_situacao`: `CHECK (((situacao) = ANY (ARRAY['IMPORTADO', 'DUPLICADO', 'REJEITADO'])))`

### `auditoria_evento`

**Módulo:** Auditoria. Trilha de alterações e de acessos a dados pessoais (LGPD). Somente inserção.

| Coluna | Tipo | Nulo | Padrão | Chave e descrição |
| --- | --- | --- | --- | --- |
| `id` | `bigint` | não | identity | PK |
| `entidade` | `character varying(60)` | não |  |  |
| `entidade_id` | `character varying(40)` | não |  |  |
| `operacao` | `character varying(20)` | não |  | CRIACAO, ALTERACAO, EXCLUSAO, VISUALIZACAO, EXPORTACAO ou ANONIMIZACAO. |
| `usuario_id` | `uuid` | sim |  | FK → `usuario` |
| `ocorrido_em` | `timestamp with time zone` | não | now() |  |
| `dados_anteriores` | `jsonb` | sim |  |  |
| `dados_novos` | `jsonb` | sim |  |  |

**Regras de integridade**

- `ck_auditoria_operacao`: `CHECK (((operacao) = ANY (ARRAY['CRIACAO', 'ALTERACAO', 'EXCLUSAO', 'VISUALIZACAO', 'EXPORTACAO', 'ANONIMIZACAO'])))`

**Índices adicionais**

- `ix_auditoria_entidade`: `btree (entidade, entidade_id, ocorrido_em DESC)`
- `ix_auditoria_usuario`: `btree (usuario_id, ocorrido_em DESC)`

## Visões e funções

| Objeto | O que entrega |
| --- | --- |
| `vw_fila_contato` | Fila de cada contrato com régua ativa: `HOJE`, `ATRASADO`, `FUTURO`, `SEM_DATA`, `SUSPENSO` ou `CANCELADO`, e os dias de atraso. |
| `vw_aniversariante` | Próximo aniversário e dias até ele, para associados com contrato vigente; 29/02 vira 28/02 em ano não bissexto. |
| `vw_carteira` | CAR-01 · Uma linha por contrato com associado, documento, cidade/UF, veículo, mensalidade, situação, etapa pendente, próximo contato, fila (de `vw_fila_contato`; sem régua ativa = `SEM_DATA`), vendedor, WhatsApp principal com opt-out e todos os telefones do associado (só para a busca). |
| `vw_historico_atendimento` | CAR-03 · Atendimentos com nome do tipo, da etapa e do autor, placa do contrato e a marca `corrigido` (já existe um registro que o corrige, ATD-04). |
| `vw_ultimo_contato_regua` | Último contato de régua concluído por contrato (ÚLTIMO CONTATO e ETAPA CONCLUÍDA da planilha). |
| `texto_busca(text)` | Remove acentos e põe em minúsculas. Busca: `texto_busca(nome) LIKE '%' \|\| texto_busca(:termo) \|\| '%'`. |

O "hoje" das visões é sempre `(now() AT TIME ZONE 'America/Sao_Paulo')::date`.

## Dados iniciais

| Tabela | Conteúdo |
| --- | --- |
| `unidade_federativa` | As 27 UFs |
| `tipo_veiculo` | `CARRO_UTILITARIO`, `CAMINHAO_MICROONIBUS`, `MOTO` |
| `tipo_contato` | `POS_VENDA`, `ANIVERSARIO`, `SUPORTE`, `INDICACAO`, `REAPROXIMACAO`, `COMERCIAL` |
| `etapa_relacionamento` | `D7` (7 dias, inicial) → `D30` (23) → `D60` (30) → `D90` (30) → `TRIMESTRAL` (90, repete) |
| `usuario` | `sistema.importacao` ("Importação da planilha", inativo, uuid `00000000-0000-0000-0000-000000000000`) |
| `municipio` | Os 5.571 municípios do IBGE (changeset `013-carga-municipio`, arquivo `dados/municipio.csv`), com índice `ix_municipio_nome_busca` sobre `texto_busca(nome)` |
