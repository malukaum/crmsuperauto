package br.com.superauto.crm.compartilhado.entidade;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import br.com.superauto.crm.erro.NegocioException;

/**
 * CPF (11 dígitos) ou CNPJ (14 caracteres, aceitando o CNPJ alfanumérico), gravado sem pontuação.
 * O tipo de pessoa vem do tamanho: 11 é pessoa física.
 *
 * <p>O construtor só confere o formato gravado (o mesmo do {@code ck_associado_documento}), para dados antigos
 * continuarem legíveis. Documento digitado entra por {@link #de(String)}, que também confere os dígitos
 * verificadores (CAR-02, proposta).
 */
public record Documento(String valor) {

    private static final Pattern CPF = Pattern.compile("^[0-9]{11}$");
    private static final Pattern CNPJ = Pattern.compile("^[0-9A-Z]{12}[0-9]{2}$");
    private static final Pattern REPETIDO = Pattern.compile("^(.)\\1+$");
    private static final int TAMANHO_CPF = 11;
    private static final int[] PESOS_CPF = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_CNPJ = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int DIGITOS_VERIFICADORES = 2;
    private static final int MODULO = 11;

    public Documento {
        Objects.requireNonNull(valor, "valor");
        if (!CPF.matcher(valor).matches() && !CNPJ.matcher(valor).matches()) {
            throw invalido();
        }
    }

    /** Documento digitado: aceita pontos, traços, barras, espaços e minúsculas, e confere os dígitos verificadores. */
    public static Documento de(String digitado) {
        String normalizado = Objects.requireNonNullElse(digitado, "").replaceAll("[.\\-/\\s]", "").toUpperCase(Locale.ROOT);
        var documento = new Documento(normalizado);
        if (REPETIDO.matcher(normalizado).matches() || !documento.digitosVerificadoresConferem()) {
            throw invalido();
        }
        return documento;
    }

    public boolean pessoaFisica() {
        return valor.length() == TAMANHO_CPF;
    }

    /** 000.000.000-00 ou AA.AAA.AAA/AAAA-00. */
    public String formatado() {
        return pessoaFisica()
                ? valor.substring(0, 3) + "." + valor.substring(3, 6) + "." + valor.substring(6, 9) + "-" + valor.substring(9)
                : valor.substring(0, 2) + "." + valor.substring(2, 5) + "." + valor.substring(5, 8) + "/"
                        + valor.substring(8, 12) + "-" + valor.substring(12);
    }

    /** Como o atendente vê (RG-04): ***.***.418-** ou **.***.***\/0001-**. */
    public String mascarado() {
        return pessoaFisica()
                ? "***.***." + valor.substring(6, 9) + "-**"
                : "**.***.***/" + valor.substring(8, 12) + "-**";
    }

    private boolean digitosVerificadoresConferem() {
        int[] pesos = pessoaFisica() ? PESOS_CPF : PESOS_CNPJ;
        int base = valor.length() - DIGITOS_VERIFICADORES;
        return digitoVerificador(base, pesos) == valor.charAt(base) - '0'
                && digitoVerificador(base + 1, pesos) == valor.charAt(base + 1) - '0';
    }

    /**
     * Módulo 11 sobre os {@code quantos} primeiros caracteres, com os pesos alinhados à direita. No CNPJ
     * alfanumérico cada caractere vale o código ASCII menos 48, o que mantém o cálculo antigo para dígitos.
     */
    private int digitoVerificador(int quantos, int[] pesos) {
        int soma = 0;
        int deslocamento = pesos.length - quantos;
        for (int i = 0; i < quantos; i++) {
            soma += (valor.charAt(i) - '0') * pesos[deslocamento + i];
        }
        int resto = soma % MODULO;
        return resto < 2 ? 0 : MODULO - resto;
    }

    private static NegocioException invalido() {
        return NegocioException.invalido("documento-invalido", "Informe um CPF ou CNPJ válido.");
    }

    @Override
    public String toString() {
        return valor;
    }
}
