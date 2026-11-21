package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.relacionamento.entidade.ResultadoContato;
import br.com.superauto.crm.relacionamento.entidade.TipoDeRelacionamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * ATD-02 · Comando de registro de um atendimento avulso (Suporte, Comercial, Reaproximação ou Indicação).
 *
 * @param contratoId vazio quando o contato é sobre a pessoa
 * @param retorno só informativo: o avulso nunca mexe na régua
 */
public record RegistroAvulso(
        Long associadoId,
        Optional<Long> contratoId,
        TipoDeRelacionamento tipo,
        ResultadoContato resultado,
        Optional<LocalDate> retorno,
        String descricao,
        Optional<LocalDateTime> realizadoEm) {

    public RegistroAvulso {
        Objects.requireNonNull(associadoId, "associadoId");
        Objects.requireNonNull(contratoId, "contratoId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(resultado, "resultado");
        Objects.requireNonNull(retorno, "retorno");
        Objects.requireNonNull(realizadoEm, "realizadoEm");
    }
}
