package br.com.superauto.crm.relacionamento.entidade;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import br.com.superauto.crm.compartilhado.entidade.Fila;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

/**
 * Uma linha da visão {@code vw_fila_contato}: a posição de um contrato com etapa pendente na régua de contato.
 * Somente leitura ({@link Immutable}): a regra de cálculo vive na visão, mantida pelo Liquibase.
 */
@Entity
@Immutable
@Table(name = "vw_fila_contato")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PosicaoFila {

    @Id
    @Column(name = "contrato_id")
    private Long contratoId;

    @Column(name = "associado_id")
    private Long associadoId;

    @Column(name = "vendedor_id")
    private UUID vendedorId;

    @Column(name = "etapa_pendente_id")
    private Short etapaPendenteId;

    @Column(name = "proximo_contato_em")
    private LocalDate proximoContatoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "fila")
    private Fila fila;

    @Column(name = "dias_atraso")
    private Integer diasAtraso;
}
