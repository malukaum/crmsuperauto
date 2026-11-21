package br.com.superauto.crm.assistente.infraestrutura.ia;

import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;
import br.com.superauto.crm.assistente.repositorio.ClienteDeIa;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/** IA-05 · Adaptador do Claude (Anthropic), com o SDK oficial em Java. */
@Component
public class ClienteClaude implements ClienteDeIa {

    @Override
    public ProvedorDeIa provedor() {
        return ProvedorDeIa.CLAUDE;
    }

    @Override
    public RespostaDaIa gerar(PedidoDaIa pedido, AcessoAoProvedor acesso) {
        AnthropicClient cliente = AnthropicOkHttpClient.builder().apiKey(acesso.chave()).build();
        try {
            Message resposta = cliente.messages().create(MessageCreateParams.builder()
                    .model(acesso.modelo())
                    .maxTokens(pedido.limiteDeTokensNaResposta())
                    .system(pedido.instrucoes())
                    .addUserMessage(pedido.conteudo())
                    .build());
            resposta.stopReason().ifPresent(ClienteClaude::exigirRespostaCompleta);
            String texto = resposta.content().stream()
                    .flatMap(bloco -> bloco.text().stream())
                    .map(TextBlock::text)
                    .collect(Collectors.joining());
            if (texto.isBlank()) throw new FalhaNoProvedor("O Claude devolveu uma resposta vazia.");
            return new RespostaDaIa(texto, resposta.usage().inputTokens(), resposta.usage().outputTokens());
        } catch (FalhaNoProvedor e) {
            throw e;
        } catch (RuntimeException e) {
            throw new FalhaNoProvedor("Claude indisponível: " + e.getMessage(), e);
        } finally {
            cliente.close();
        }
    }

    private static void exigirRespostaCompleta(StopReason motivo) {
        if (StopReason.REFUSAL.equals(motivo)) throw new FalhaNoProvedor("O Claude recusou a solicitação.");
        if (StopReason.MAX_TOKENS.equals(motivo)) throw new FalhaNoProvedor("A resposta do Claude foi cortada.");
    }
}
