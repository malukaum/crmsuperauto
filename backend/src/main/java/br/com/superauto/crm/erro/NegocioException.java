package br.com.superauto.crm.erro;

import java.util.Map;

import org.springframework.http.HttpStatus;

/**
 * Erro previsto de regra de negócio. Sai como ProblemDetail com o status e a mensagem informados;
 * {@code codigo} e {@code dados} viram propriedades extras para o front decidir o que fazer.
 */
public class NegocioException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final Map<String, Object> dados;

    public NegocioException(HttpStatus status, String codigo, String mensagem, Map<String, Object> dados) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
        this.dados = dados == null ? Map.of() : Map.copyOf(dados);
    }

    public NegocioException(HttpStatus status, String codigo, String mensagem) {
        this(status, codigo, mensagem, null);
    }

    public static NegocioException naoEncontrado(String mensagem) {
        return new NegocioException(HttpStatus.NOT_FOUND, "nao-encontrado", mensagem);
    }

    public static NegocioException conflito(String codigo, String mensagem) {
        return new NegocioException(HttpStatus.CONFLICT, codigo, mensagem);
    }

    public static NegocioException invalido(String codigo, String mensagem) {
        return new NegocioException(HttpStatus.UNPROCESSABLE_CONTENT, codigo, mensagem);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public Map<String, Object> getDados() {
        return dados;
    }
}
