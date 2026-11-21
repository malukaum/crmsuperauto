# Assistente de IA (IA)

> [Índice das histórias](README.md) · Regras gerais: [RG-01 a RG-14](README.md#regras-gerais) · [Modelo de dados](../modelo-de-dados/README.md)

> **Protótipo funcional (21/11/2026).** Implementado de ponta a ponta para demonstração, em cima da [Inteligência comercial (INT)](06-inteligencia.md). Veja [o que o protótipo cobre](#o-que-o-protótipo-cobre).

O assistente transforma o que a equipe anota livremente nos atendimentos (texto e anexos) em um **resumo estruturado** por atendimento. Os resumos de todos os clientes alimentam indicadores de tendência e recomendações de abordagem. Os números sempre saem do banco; a IA resume, classifica e sugere, mas nunca inventa número nem age sozinha.

**Decisões do módulo (21/11/2026):**

- A IA é **contratada** (serviço externo). O sistema trabalha com **Claude (Anthropic), Gemini (Google) e ChatGPT (OpenAI)**, e o provedor é escolhido por tarefa, com um reserva para falhas ([IA-05](#ia-05--configurar-os-provedores-de-ia)). Trocar de provedor não muda o resto do sistema: cada um é um adaptador atrás da mesma porta.
- O CRM será vendido a outras empresas no futuro: **cada empresa configura os próprios provedores, modelos e chaves**, e nada de provedor fica fixo no código.
- Antes de qualquer envio, os dados são **pseudonimizados** ([IA-06](#ia-06--proteger-os-dados-pessoais-lgpd)): nome, CPF/CNPJ, placa, telefone e e-mail viram marcadores, e só o nosso servidor sabe a quem eles se referem.
- O banco guarda o **resumo estruturado**, não o material bruto, para crescer pouco e servir à inteligência.

### IA-01 · Anotar o atendimento com texto livre e anexos

**Como** membro da equipe, **quero** anotar livremente o que conversei e anexar arquivos ao registrar um atendimento, **para** não perder detalhes que não cabem nos campos do sistema.

**Regras**

- No registro do atendimento ([ATD-01](04-atendimentos.md#atd-01--registrar-atendimento-de-pós-venda), [ATD-02](04-atendimentos.md#atd-02--registrar-atendimento-avulso)), além do resumo atual, há um campo de anotações livres (até 10.000 caracteres) e anexos opcionais.
- Anexos aceitos: PDF, imagens (JPG, PNG) e texto. Até 5 arquivos de até 10 MB cada **(proposta)**.
- O registro do atendimento não espera a IA: grava na hora e o processamento acontece em segundo plano.
- O atendimento mostra a situação do processamento: Aguardando, Resumido ou Não foi possível resumir (com nova tentativa automática).
- Com o assistente desligado ([IA-05](#ia-05--configurar-os-provedores-de-ia)), o campo de anotações continua disponível e as anotações aguardam até ele ser ligado.

**Critérios de aceite**

- Dado um atendimento registrado com anotações e um PDF, então ele aparece na hora no histórico, com o processamento "Aguardando".
- Dado o provedor de IA fora do ar, quando registro um atendimento, então o registro é gravado normalmente e o resumo é tentado de novo depois.

### IA-02 · Gerar e guardar o resumo estruturado

**Como** equipe, **quero** que o sistema resuma cada atendimento anotado num formato padrão, **para** consultar rápido e permitir indicadores comparáveis entre clientes.

**Regras**

- Para cada atendimento com anotações ou anexos, o assistente produz um resumo estruturado com: resumo curto (até 500 caracteres), assuntos tratados (lista fechada, ex.: assistência 24h, guincho, mensalidade, sinistro, cancelamento, indicação), satisfação (positiva, neutra, negativa), objeções, interesses, sinais de risco de cancelamento, chance de indicar e próxima ação sugerida.
- A resposta da IA é validada contra esse formato; resposta fora do formato é recusada e tentada de novo.
- O texto dos anexos é extraído no nosso servidor (PDF por leitura do texto; imagem por OCR ou pelo provedor, sempre depois de pseudonimizar) **(proposta)**.
- Depois de gerado o resumo, os anexos originais são apagados; fica só o tipo, a data e uma impressão digital (hash) do arquivo **(proposta)**. As anotações livres ficam no atendimento, como o resumo atual.
- O resumo aparece na ficha, junto do atendimento, marcado como gerado pela IA. Quem tem acesso ao atendimento pode corrigir os campos do resumo; a correção substitui a da IA e fica na auditoria.
- O resumo nunca altera a régua, a situação do contrato nem a classificação do cliente sozinho.

**Critérios de aceite**

- Dado um atendimento anotado com "cliente reclamou da demora do guincho, pensa em cancelar", quando o resumo fica pronto, então tem o assunto "guincho", satisfação negativa e sinal de risco de cancelamento.
- Dado um resumo com o assunto errado, quando a atendente o corrige, então o novo valor vale para os indicadores e a correção fica na auditoria.

### IA-03 · Ver tendências da carteira

**Como** gerencial ou vendedor, **quero** ver o que os clientes mais comentam e onde está o risco, **para** agir antes de perder contratos.

**Regras**

- Indicadores calculados no banco a partir dos resumos estruturados, por período: assuntos mais frequentes, satisfação ao longo do tempo, objeções mais comuns, clientes com sinal de risco e clientes com chance alta de indicar.
- Recortes por cidade, tipo de veículo, etapa da régua e vendedor.
- Uma leitura em texto, gerada pela IA a partir apenas dos números agregados (sem dados de cliente), explica as principais tendências do período.
- Gerencial e atendente veem toda a carteira; o vendedor só a sua ([RG-03](README.md#regras-gerais)). Painel para gerencial e vendedor **(proposta)**.
- Cada indicador abre a lista de atendimentos ou a carteira filtrada correspondente.

**Critérios de aceite**

- Dados 10 atendimentos no mês, 4 deles com o assunto "guincho", então "guincho" aparece com 40% dos atendimentos do mês.
- Dado o vendedor, quando abre o painel, então os números consideram só os contratos dele.

### IA-04 · Receber recomendações de abordagem

**Como** membro da equipe, **quero** sugestões de como abordar o cliente, **para** atender melhor com base no que funcionou com clientes parecidos.

**Regras**

- Ao abrir a ficha ou registrar um atendimento, o sistema mostra até 3 recomendações, com base nos resumos do próprio cliente e nas tendências agregadas ([IA-03](#ia-03--ver-tendências-da-carteira)). Ex.: "Clientes que reclamaram do guincho voltaram a ficar satisfeitos depois de um retorno em até 2 dias".
- Recomendação é só sugestão: nunca envia mensagem ([RG-10](README.md#regras-gerais)), nunca altera régua, contrato ou classificação.
- Cada recomendação mostra em que se baseia (quais indicadores ou atendimentos).
- A equipe pode marcar uma recomendação como útil ou não útil, para medir a qualidade do assistente.
- Disponível para toda a equipe, no escopo de cada papel ([RG-03](README.md#regras-gerais)) **(proposta)**.

**Critérios de aceite**

- Dado um cliente com sinal de risco no último atendimento, quando abro a ficha, então vejo ao menos uma recomendação de retenção, com a base dela.
- Dado o assistente desligado, então a ficha não mostra recomendações e funciona normalmente.

### IA-05 · Configurar os provedores de IA

**Como** gerencial, **quero** configurar Claude, Gemini e ChatGPT e escolher qual atende cada tarefa, **para** equilibrar qualidade, custo e disponibilidade sem depender de programador.

**Regras**

- Só o gerencial configura. Cada empresa que usa o CRM tem a sua própria configuração.
- Provedores disponíveis: Claude (Anthropic), Gemini (Google) e ChatGPT (OpenAI). Para cada um: chave de acesso, modelo, ligado ou desligado e limite de gasto mensal. Provedor novo entra como mais um adaptador, sem mudar as telas nem as regras.
- Para cada tarefa do assistente (resumo do atendimento, leitura das tendências, recomendações), o gerencial escolhe um **provedor principal** e um **provedor reserva**.
- Se o principal falhar (fora do ar, chave inválida, resposta fora do formato depois das novas tentativas) ou atingir o limite do mês, o reserva assume. A troca fica registrada e o gerencial é avisado. Sem reserva disponível, a tarefa fica "Aguardando".
- O resultado tem o mesmo formato qualquer que seja o provedor: o sistema valida o resumo e as recomendações contra o mesmo formato e guarda qual provedor e modelo gerou cada um.
- A chave é guardada cifrada no servidor, nunca aparece de volta na tela (só os últimos caracteres) e nunca vai para o navegador; as chamadas à IA saem só do servidor.
- "Testar conexão" por provedor verifica a chave e o modelo com uma chamada mínima, sem dados de cliente.
- A tela mostra o uso do mês por provedor (tarefas atendidas, custo estimado, falhas, vezes em que o reserva assumiu) e a avaliação das recomendações ([IA-04](#ia-04--receber-recomendações-de-abordagem)) por provedor, para comparar.
- A pseudonimização ([IA-06](#ia-06--proteger-os-dados-pessoais-lgpd)) vale igual para todos os provedores.
- Toda mudança de configuração vai para a auditoria ([RG-08](README.md#regras-gerais)), sem a chave.

**Critérios de aceite**

- Dada uma chave inválida, quando clico em "Testar conexão", então o sistema avisa que a conexão falhou, sem salvar a chave.
- Dado o resumo configurado com Claude como principal e Gemini como reserva, quando o Claude está fora do ar, então o resumo é feito pelo Gemini, fica marcado como gerado por ele, e o gerencial vê o aviso da troca.
- Dado o principal com o limite mensal atingido e o reserva também, então os novos atendimentos ficam "Aguardando" e o gerencial vê o aviso de pausa.

### IA-06 · Proteger os dados pessoais (LGPD)

**Como** responsável pelos dados, **quero** que o assistente envie ao fornecedor só o necessário e sem identificar o cliente, **para** cumprir a LGPD.

**Regras**

- Antes de enviar, o sistema troca nome, CPF/CNPJ, placa, telefone, e-mail e endereço por marcadores (ex.: [CLIENTE], [PLACA_1]) e só reconstrói a resposta no nosso servidor. Isso vale para as anotações e para o texto extraído dos anexos.
- Anexos com documento de identidade ou dados bancários não são enviados ao fornecedor **(proposta)**.
- Só o conteúdo do atendimento é enviado: nada da ficha, da carteira ou de outros clientes. A leitura de tendências ([IA-03](#ia-03--ver-tendências-da-carteira)) recebe apenas números agregados.
- O uso exige contrato de tratamento de dados com o fornecedor, com a retenção mínima do lado dele, e o registro dessa base legal **(a validar)**.
- Anonimizar o cliente ([CAR-08](02-carteira.md#car-08--anonimizar-cliente-lgpd)) apaga também as anotações, os anexos e os resumos dele; os indicadores agregados já calculados continuam.
- Cada envio ao fornecedor fica na auditoria (atendimento, data, provedor, modelo), sem o conteúdo.

**Critérios de aceite**

- Dada uma anotação com "José da Silva, placa HAA7216, fone 37 99869-2950", quando ela é enviada ao fornecedor, então o texto enviado contém só os marcadores, e o resumo guardado volta com o nome e a placa corretos.
- Dado um cliente anonimizado, então as anotações e os resumos dele não aparecem mais em lugar nenhum, e os totais do painel do mês continuam os mesmos.

### O que o protótipo cobre

**Pronto:**

- IA-01: anotações livres (até 10.000 caracteres) e até 5 anexos de 10 MB (PDF, JPG, PNG, texto) no registro pela ficha; gravação na hora e resumo em segundo plano a cada 20 segundos, com até 5 tentativas (espera de 1, 5, 15 e 60 min). Com o assistente desligado ou sem provedor no limite, a anotação espera sem gastar tentativa. Uma anotação por atendimento (`anotacao-existente`).
- IA-02: resumo estruturado validado no servidor (mesmo formato para qualquer provedor), marcado com provedor e modelo; resposta fora do formato passa para o reserva. Texto de PDF e de arquivo de texto extraído no nosso servidor (Apache PDFBox). Conteúdo dos anexos apagado depois do resumo ou da desistência.
- IA-03: painel de tendências em Inteligência (assuntos, satisfação, risco, chance de indicar), por período (padrão: últimos 30 dias; até 1 ano), com o escopo do vendedor; leitura em texto pela IA a pedido, só com números agregados.
- IA-04: "Sugerir abordagem" na ficha, com até 3 sugestões e a base de cada uma, a partir dos 10 resumos mais recentes do cliente e dos indicadores da carteira de quem pede.
- IA-05: tela Configurações › Assistente de IA, com modelo, chave (cifrada, só os 4 últimos caracteres voltam), ligado, limite mensal e preços por provedor; testar conexão sem salvar; principal e reserva por tarefa; uso do mês (chamadas, falhas, vezes como reserva, custo estimado). Mudanças auditadas sem a chave.
- IA-06: pseudonimização de nome (com e sem acento, partes do nome), e-mail, CPF/CNPJ, placa e telefone antes de todo envio; restauração só no servidor; anonimizar o cliente apaga anotações, anexos e textos dos resumos, mantendo assunto, satisfação, risco e chance de indicar nos totais. Cada chamada fica em `uso_ia`, sem o conteúdo.

**Fora do protótipo (próximos passos):**

- Imagens não vão ao provedor (sem OCR no servidor ainda, não há como pseudonimizá-las); o resumo registra que a imagem foi recebida.
- Anotação pela fila de trabalho (hoje só pela ficha) e correção dos campos do resumo pela equipe (IA-02).
- Marcar recomendação como útil ou não útil e a avaliação por provedor (IA-04, IA-05).
- Aviso ao gerencial quando o reserva assume ou quando a tarefa pausa por limite (hoje aparece no uso do mês).
- Recortes das tendências por cidade, tipo de veículo e etapa, e o clique do indicador abrindo a lista filtrada (IA-03).
- Detecção de documento de identidade ou dado bancário nos anexos (IA-06) e registro da base legal com o fornecedor.
- Configuração por empresa (multiempresa): hoje há uma configuração para a instalação.
