package br.com.superauto.crm.cadastro.entidade;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

import br.com.superauto.crm.erro.NegocioException;

/**
 * Valor mensal contratado de um contrato, em reais, com duas casas. É valor contratado, não recebido (RG-09).
 */
public record Mensalidade(BigDecimal valor) {

    private static final int CASAS_DECIMAIS = 2;

    public Mensalidade {
        Objects.requireNonNull(valor, "valor");
        if (valor.signum() < 0) {
            throw NegocioException.invalido("mensalidade-negativa", "A mensalidade não pode ser negativa.");
        }
        if (valor.stripTrailingZeros().scale() > CASAS_DECIMAIS) {
            throw NegocioException.invalido("mensalidade-centavos", "A mensalidade aceita no máximo dois decimais.");
        }
        valor = valor.setScale(CASAS_DECIMAIS, RoundingMode.UNNECESSARY);
    }

    public static Mensalidade de(String valor) {
        return new Mensalidade(new BigDecimal(valor));
    }
}
