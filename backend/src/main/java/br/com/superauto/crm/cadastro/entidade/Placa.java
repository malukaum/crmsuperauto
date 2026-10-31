package br.com.superauto.crm.cadastro.entidade;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

import br.com.superauto.crm.erro.NegocioException;

/**
 * Placa do veículo no padrão antigo (ABC1234) ou Mercosul (ABC1D23), sempre em maiúsculas e sem hífen (RG-02).
 * Aceita a placa digitada com hífen, espaços ou minúsculas.
 */
public record Placa(String valor) {

    private static final Pattern FORMATO = Pattern.compile("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$");

    public Placa {
        valor = Objects.requireNonNullElse(valor, "").replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
        if (!FORMATO.matcher(valor).matches()) {
            throw NegocioException.invalido("placa-invalida",
                    "Informe a placa no padrão antigo (ABC1234) ou Mercosul (ABC1D23).");
        }
    }

    public static Placa de(String digitada) {
        return new Placa(digitada);
    }

    @Override
    public String toString() {
        return valor;
    }
}
