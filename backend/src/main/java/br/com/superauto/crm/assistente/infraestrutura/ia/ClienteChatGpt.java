package br.com.superauto.crm.assistente.infraestrutura.ia;

import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;
import br.com.superauto.crm.assistente.repositorio.ClienteDeIa;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.completions.CompletionUsage;

import org.springframework.stereotype.Component;

/** IA-05 · Adaptador do ChatGPT (OpenAI), com o SDK oficial em Java. */
@Component
public class ClienteChatGpt implements ClienteDeIa {

    @Override
    public ProvedorDeIa provedor() {
        return ProvedorDeIa.CHATGPT;
    }

    @Override
    public RespostaDaIa gerar(PedidoDaIa pedido, AcessoAoProvedor acesso) {
        OpenAIClient cliente = OpenAIOkHttpClient.builder().apiKey(acesso.chave()).build();
        try {
            ChatCompletion resposta = cliente.chat().completions().create(ChatCompletionCreateParams.builder()
                    .model(acesso.modelo())
                    .maxCompletionTokens(pedido.limiteDeTokensNaResposta())
                    .addSystemMessage(pedido.instrucoes())
                    .addUserMessage(pedido.conteudo())
                    .build());
            if (resposta.choices().isEmpty()) throw new FalhaNoProvedor("O ChatGPT não devolveu resposta.");
            var escolha = resposta.choices().getFirst();
            if (ChatCompletion.Choice.FinishReason.CONTENT_FILTER.equals(escolha.finishReason()))
                throw new FalhaNoProvedor("O ChatGPT recusou a solicitação.");
            if (ChatCompletion.Choice.FinishReason.LENGTH.equals(escolha.finishReason()))
                throw new FalhaNoProvedor("A resposta do ChatGPT foi cortada.");
            if (escolha.message().refusal().isPresent()) throw new FalhaNoProvedor("O ChatGPT recusou a solicitação.");
            String texto = escolha.message().content().filter(t -> !t.isBlank())
                    .orElseThrow(() -> new FalhaNoProvedor("O ChatGPT devolveu uma resposta vazia."));
            return new RespostaDaIa(texto, resposta.usage().map(CompletionUsage::promptTokens).orElse(0L),
                    resposta.usage().map(CompletionUsage::completionTokens).orElse(0L));
        } catch (FalhaNoProvedor e) {
            throw e;
        } catch (RuntimeException e) {
            throw new FalhaNoProvedor("ChatGPT indisponível: " + e.getMessage(), e);
        } finally {
            cliente.close();
        }
    }
}
