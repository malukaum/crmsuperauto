package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.relacionamento.entidade.PeriodoDoHistorico;
import br.com.superauto.crm.relacionamento.repositorio.FiltroDoHistorico;

import java.util.Objects;

/** ATD-05 · O que consultar no histórico: o período e os filtros. */
public record ConsultaDoHistorico(PeriodoDoHistorico periodo, FiltroDoHistorico filtro) {

    public ConsultaDoHistorico {
        Objects.requireNonNull(periodo, "periodo");
        Objects.requireNonNull(filtro, "filtro");
    }
}
