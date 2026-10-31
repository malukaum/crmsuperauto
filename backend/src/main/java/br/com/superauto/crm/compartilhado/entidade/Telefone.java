package br.com.superauto.crm.compartilhado.entidade;

import java.util.Objects;
import java.util.regex.Pattern;

import br.com.superauto.crm.erro.NegocioException;

/**
 * Telefone brasileiro gravado como 55 + DDD + número, só dígitos (ex.: {@code 5537998692950}). Objeto de valor
 * do núcleo compartilhado: usado nos contatos do associado (CAR-04) e nas indicações (IND-01).
 *
 * <p>O construtor só confere o formato gravado; quem recebe o número digitado usa {@link #de(String)}, que aceita
 * com ou sem 55, parênteses, espaços e traços, e exige o DDD.
 */
public record Telefone(String valor) {

    private static final String DDI_BRASIL = "55";
    private static final Pattern GRAVADO = Pattern.compile("^55[1-9]{2}[0-9]{8,9}$");
    private static final Pattern NACIONAL = Pattern.compile("^[1-9]{2}[0-9]{8,9}$");
    private static final int TAMANHO_NACIONAL_FIXO = 10;
    private static final int TAMANHO_NACIONAL_CELULAR = 11;
    private static final int TAMANHO_DDD = 2;
    private static final int INICIO_DDD = DDI_BRASIL.length();
    private static final int INICIO_NUMERO = INICIO_DDD + TAMANHO_DDD;

    public Telefone {
        Objects.requireNonNull(valor, "valor");
        if (!GRAVADO.matcher(valor).matches()) {
            throw invalido();
        }
    }

    /**
     * Normaliza o número digitado: "(37) 99869-2950", "+55 37 99869-2950", "037 99869-2950" e "37998692950" viram o
     * mesmo telefone. O zero de discagem à frente do DDD é descartado.
     */
    public static Telefone de(String digitado) {
        String digitos = Objects.requireNonNullElse(digitado, "").replaceAll("\\D", "").replaceFirst("^0+", "");
        String nacional = temDdi(digitos) ? digitos.substring(DDI_BRASIL.length()) : digitos;
        if (!NACIONAL.matcher(nacional).matches()) {
            throw invalido();
        }
        return new Telefone(DDI_BRASIL + nacional);
    }

    /** Com 12 ou 13 dígitos começando por 55, o 55 é o código do país; com 10 ou 11, 55 seria o DDD (RS). */
    private static boolean temDdi(String digitos) {
        int semDdi = digitos.length() - DDI_BRASIL.length();
        return digitos.startsWith(DDI_BRASIL) && (semDdi == TAMANHO_NACIONAL_FIXO || semDdi == TAMANHO_NACIONAL_CELULAR);
    }

    public String ddd() {
        return valor.substring(INICIO_DDD, INICIO_NUMERO);
    }

    public boolean celular() {
        return valor.length() - INICIO_NUMERO == TAMANHO_NACIONAL_CELULAR - TAMANHO_DDD;
    }

    /** Formato de exibição da RG-02: (37) 9 9869-2950 para celular, (37) 3321-1234 para fixo. */
    public String formatado() {
        String numero = valor.substring(INICIO_NUMERO);
        String prefixo = celular() ? numero.charAt(0) + " " + numero.substring(1, 5) : numero.substring(0, 4);
        return "(" + ddd() + ") " + prefixo + "-" + numero.substring(numero.length() - 4);
    }

    private static NegocioException invalido() {
        return NegocioException.invalido("telefone-invalido",
                "Informe o telefone com DDD, por exemplo (37) 9 9869-2950.");
    }

    @Override
    public String toString() {
        return valor;
    }
}
