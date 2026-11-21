package br.com.superauto.crm.relacionamento.entidade;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * O que o ajuste da régua pelo gerencial precisa saber (ATD-04).
 *
 * @param baseRegua o dia do último contato concluído ou, sem nenhum, da adesão (RG-14)
 */
public record ContextoDoAjuste(LocalDate hoje, LocalDate baseRegua, List<EtapaRegua> etapas) {

    public ContextoDoAjuste {
        Objects.requireNonNull(hoje, "hoje");
        Objects.requireNonNull(baseRegua, "baseRegua");
        etapas = List.copyOf(etapas);
    }
}
