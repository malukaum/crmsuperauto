package br.com.superauto.crm.assistente.infraestrutura.ia;

import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;
import br.com.superauto.crm.assistente.repositorio.ClienteDeIa;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.FinishReason;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GenerateContentResponseUsageMetadata;
import com.google.genai.types.Part;

import org.springframework.stereotype.Component;

import java.util.Set;

/** IA-05 · Adaptador do Gemini (Google), com o SDK oficial em Java. */
@Component
public class ClienteGemini implements ClienteDeIa {
    private static final Set<FinishReason.Known> RECUSAS = Set.of(FinishReason.Known.SAFETY,
            FinishReason.Known.PROHIBITED_CONTENT, FinishReason.Known.IMAGE_SAFETY,
            FinishReason.Known.IMAGE_PROHIBITED_CONTENT);

    @Override
    public ProvedorDeIa provedor() {
        return ProvedorDeIa.GEMINI;
    }

    @Override
    public RespostaDaIa gerar(PedidoDaIa pedido, AcessoAoProvedor acesso) {
        Client cliente = Client.builder().apiKey(acesso.chave()).build();
        try {
            GenerateContentResponse resposta = cliente.models.generateContent(acesso.modelo(), pedido.conteudo(),
                    GenerateContentConfig.builder()
                            .systemInstruction(Content.fromParts(Part.fromText(pedido.instrucoes())))
                            .maxOutputTokens((int) pedido.limiteDeTokensNaResposta())
                            .build());
            exigirRespostaCompleta(resposta.finishReason());
            String texto = resposta.text();
            if (texto == null || texto.isBlank()) throw new FalhaNoProvedor("O Gemini devolveu uma resposta vazia.");
            var uso = resposta.usageMetadata();
            return new RespostaDaIa(texto,
                    uso.flatMap(GenerateContentResponseUsageMetadata::promptTokenCount).orElse(0),
                    uso.flatMap(GenerateContentResponseUsageMetadata::candidatesTokenCount).orElse(0));
        } catch (FalhaNoProvedor e) {
            throw e;
        } catch (RuntimeException e) {
            throw new FalhaNoProvedor("Gemini indisponível: " + e.getMessage(), e);
        } finally {
            cliente.close();
        }
    }

    private static void exigirRespostaCompleta(FinishReason motivo) {
        if (motivo == null) return;
        if (RECUSAS.contains(motivo.knownEnum())) throw new FalhaNoProvedor("O Gemini recusou a solicitação.");
        if (FinishReason.Known.MAX_TOKENS.equals(motivo.knownEnum()))
            throw new FalhaNoProvedor("A resposta do Gemini foi cortada.");
    }
}
