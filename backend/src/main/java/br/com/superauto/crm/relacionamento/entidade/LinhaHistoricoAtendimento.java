package br.com.superauto.crm.relacionamento.entidade;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

/**
 * Modelo de leitura do histórico de atendimentos (CAR-03, ATD-05): uma linha da visão
 * {@code vw_historico_atendimento}, com os nomes de tipo, etapa e autor.
 */
@Entity
@Immutable
@Table(name = "vw_historico_atendimento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinhaHistoricoAtendimento {

    @Id
    private Long id;

    @Column(name = "associado_id")
    private Long associadoId;

    @Column(name = "contrato_id")
    private Long contratoId;

    private String placa;

    private String tipo;

    private String etapa;

    private String resultado;

    private String descricao;

    @Column(name = "retorno_agendado_para")
    private LocalDate retornoAgendadoPara;

    @Column(name = "realizado_em")
    private OffsetDateTime realizadoEm;

    @Column(name = "registrado_por")
    private String registradoPor;

    @Column(name = "corrige_atendimento_id")
    private Long corrigeAtendimentoId;

    private boolean corrigido;

    public Optional<Long> getContratoId() {
        return Optional.ofNullable(contratoId);
    }

    public Optional<String> getPlaca() {
        return Optional.ofNullable(placa);
    }

    public Optional<String> getEtapa() {
        return Optional.ofNullable(etapa);
    }

    public Optional<String> getDescricao() {
        return Optional.ofNullable(descricao);
    }

    public Optional<LocalDate> getRetornoAgendadoPara() {
        return Optional.ofNullable(retornoAgendadoPara);
    }

    public Optional<Long> getCorrigeAtendimentoId() {
        return Optional.ofNullable(corrigeAtendimentoId);
    }
}
