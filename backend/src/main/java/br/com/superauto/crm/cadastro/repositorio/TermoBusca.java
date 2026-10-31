package br.com.superauto.crm.cadastro.repositorio;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * O que a pessoa digitou na busca da carteira (RG-05), lido de três jeitos: o texto como veio (nome, comparado sem
 * acento pelo banco), só letras e números em maiúsculas (placa, CPF/CNPJ) e só dígitos (telefone). Assim
 * "HAA-7216", "haa7216" e "103.034" funcionam.
 */
public record TermoBusca(String texto) {

    /** Trechos menores que isso encontrariam quase toda a carteira por documento ou telefone. */
    private static final int MINIMO_CARACTERES_CODIGO = 3;

    public TermoBusca {
        texto = Objects.requireNonNull(texto, "texto").strip();
    }

    public static Optional<TermoBusca> de(String digitado) {
        return Optional.ofNullable(digitado).map(String::strip).filter(t -> !t.isEmpty()).map(TermoBusca::new);
    }

    /** Para placa e documento: só letras e dígitos, em maiúsculas. */
    public Optional<String> alfanumerico() {
        return codigo(texto.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT));
    }

    /** Para telefone: só dígitos. */
    public Optional<String> digitos() {
        return codigo(texto.replaceAll("\\D", ""));
    }

    private static Optional<String> codigo(String valor) {
        return valor.length() >= MINIMO_CARACTERES_CODIGO ? Optional.of(valor) : Optional.empty();
    }
}
