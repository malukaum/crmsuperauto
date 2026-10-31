package br.com.superauto.crm.comercial.entidade;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Modelo de leitura de uma indicação, para a ficha do associado indicador (CAR-03). O agregado {@code Indicacao},
 * com o registro e o andamento, chega com a IND-01.
 */
@Entity
@Immutable
@Table(name = "indicacao")
public class IndicacaoResumida {

    @Id
    private Long id;

    @Column(name = "indicador_id")
    private Long indicadorId;

    @Column(name = "indicado_nome")
    private String indicadoNome;

    private String status;

    @Column(name = "registrada_em")
    private OffsetDateTime registradaEm;

    protected IndicacaoResumida() {
    }

    public Long getId() {
        return id;
    }

    public Long getIndicadorId() {
        return indicadorId;
    }

    public String getIndicadoNome() {
        return indicadoNome;
    }

    public String getStatus() {
        return status;
    }

    public OffsetDateTime getRegistradaEm() {
        return registradaEm;
    }
}
