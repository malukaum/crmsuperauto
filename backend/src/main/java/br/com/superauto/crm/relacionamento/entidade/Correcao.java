package br.com.superauto.crm.relacionamento.entidade;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** ATD-04 · O que muda na correção de um atendimento; contrato, tipo, etapa e data continuam os do original. */
public record Correcao(ResultadoContato resultado, Optional<LocalDate> retorno, String descricao) {

    public Correcao {
        Objects.requireNonNull(resultado, "resultado");
        Objects.requireNonNull(retorno, "retorno");
    }
}
