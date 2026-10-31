package br.com.superauto.crm.compartilhado.entidade;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import br.com.superauto.crm.erro.NegocioException;

/**
 * Endereço de e-mail, sempre normalizado (sem espaços nas pontas e em minúsculas) e válido.
 * Objeto de valor do núcleo compartilhado: usado por acesso (usuário) e cadastro (contato do associado).
 */
public record Email(String valor) {

    public static final int TAMANHO_MAXIMO = 254;

    private static final Pattern FORMATO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        valor = Objects.requireNonNullElse(valor, "").trim().toLowerCase(Locale.ROOT);
        if (valor.length() > TAMANHO_MAXIMO || !FORMATO.matcher(valor).matches()) {
            throw NegocioException.invalido("email-invalido", "Informe um e-mail válido.");
        }
    }

    public static Email de(String texto) {
        return new Email(texto);
    }

    /**
     * Para dados vindos de fora (token, Keycloak), onde e-mail ausente ou fora do padrão não deve
     * interromper a operação: devolve vazio em vez de lançar.
     */
    public static Optional<Email> seValido(String texto) {
        if (texto == null || texto.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Email(texto));
        } catch (NegocioException e) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return valor;
    }
}
