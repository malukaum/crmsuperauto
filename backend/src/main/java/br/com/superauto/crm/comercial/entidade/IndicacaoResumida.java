package br.com.superauto.crm.comercial.entidade;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

/**
 * Modelo de leitura de uma indicação, para a ficha do associado indicador (CAR-03). O agregado {@code Indicacao},
 * com o registro e o andamento, chega com a IND-01.
 */
@Entity
@Immutable
@Table(name = "indicacao")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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
}
