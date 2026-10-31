package br.com.superauto.crm.erro;

import java.util.Map;

/**
 * Erro previsto de regra de negócio. Não conhece HTTP: o {@link Tipo} diz a natureza do erro e o
 * {@link GlobalExceptionHandler} decide como ele sai na API. {@code codigo} e {@code dados} viram
 * propriedades extras para o front decidir o que fazer.
 */
public class NegocioException extends RuntimeException {

    public enum Tipo {
        /** O recurso não existe (ou não é visível para esta operação). */
        NAO_ENCONTRADO,
        /** O estado atual impede a operação (ex.: duplicidade, pendência a resolver antes). */
        CONFLITO,
        /** Os dados ou a operação pedida violam uma regra. */
        INVALIDO
    }

    private final Tipo tipo;
    private final String codigo;
    private final Map<String, Object> dados;

    public NegocioException(Tipo tipo, String codigo, String mensagem, Map<String, Object> dados) {
        super(mensagem);
        this.tipo = tipo;
        this.codigo = codigo;
        this.dados = dados == null ? Map.of() : Map.copyOf(dados);
    }

    public NegocioException(Tipo tipo, String codigo, String mensagem) {
        this(tipo, codigo, mensagem, null);
    }

    public static NegocioException naoEncontrado(String mensagem) {
        return new NegocioException(Tipo.NAO_ENCONTRADO, "nao-encontrado", mensagem);
    }

    public static NegocioException conflito(String codigo, String mensagem) {
        return new NegocioException(Tipo.CONFLITO, codigo, mensagem);
    }

    public static NegocioException conflito(String codigo, String mensagem, Map<String, Object> dados) {
        return new NegocioException(Tipo.CONFLITO, codigo, mensagem, dados);
    }

    public static NegocioException invalido(String codigo, String mensagem) {
        return new NegocioException(Tipo.INVALIDO, codigo, mensagem);
    }

    public Tipo getTipo() {
        return tipo;
    }

    public String getCodigo() {
        return codigo;
    }

    public Map<String, Object> getDados() {
        return dados;
    }
}
