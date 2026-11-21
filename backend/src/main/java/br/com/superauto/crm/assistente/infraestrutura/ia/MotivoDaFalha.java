package br.com.superauto.crm.assistente.infraestrutura.ia;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IA-05 · Traduz o erro HTTP de um provedor numa frase curta em português para o gerencial, sem o JSON bruto.
 * Camada anticorrupção: o formato de erro de cada SDK não sai do adaptador.
 */
final class MotivoDaFalha {
    private static final Pattern MENSAGEM = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");
    private static final int TAMANHO_DO_DETALHE = 160;

    private MotivoDaFalha() {
    }

    static String de(String provedor, int status, String detalhe) {
        String mensagem = mensagemDe(detalhe);
        return provedor + ": " + motivo(status, mensagem);
    }

    private static String motivo(int status, String mensagem) {
        if (semCredito(mensagem)) return "sem crédito na conta do provedor. Confira o faturamento no site dele.";
        return switch (status) {
            case 401 -> "chave de acesso inválida.";
            case 402 -> "sem crédito na conta do provedor. Confira o faturamento no site dele.";
            case 403 -> "a chave não tem permissão para usar este modelo.";
            case 404 -> "modelo não encontrado. Confira o nome do modelo.";
            case 408, 504 -> "o provedor demorou demais para responder.";
            case 429 -> "limite de uso do provedor atingido. Tente de novo em alguns minutos.";
            default -> status >= 500
                    ? "o provedor está fora do ar ou sobrecarregado."
                    : "o provedor recusou o pedido (" + status + ")" + (mensagem.isEmpty() ? "." : ": " + mensagem);
        };
    }

    private static boolean semCredito(String mensagem) {
        String texto = mensagem.toLowerCase(Locale.ROOT);
        return texto.contains("credit balance") || texto.contains("insufficient_quota") || texto.contains("billing");
    }

    /** Fica com o campo "message" do JSON de erro, quando houver, e corta textos longos. */
    private static String mensagemDe(String detalhe) {
        if (detalhe == null) return "";
        Matcher m = MENSAGEM.matcher(detalhe);
        String mensagem = (m.find() ? m.group(1) : detalhe).strip();
        return mensagem.length() > TAMANHO_DO_DETALHE ? mensagem.substring(0, TAMANHO_DO_DETALHE) + "…" : mensagem;
    }
}
