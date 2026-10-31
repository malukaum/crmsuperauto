package br.com.superauto.crm.referencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

/** Município da tabela oficial do IBGE. Carregado pelo Liquibase e nunca alterado pela aplicação. */
@Entity
@Immutable
@Table(name = "municipio")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Municipio {

    @Id
    private Integer id;

    @Column(name = "codigo_ibge", nullable = false, length = 7, columnDefinition = "bpchar(7)")
    private String codigoIbge;

    @Column(nullable = false, length = 80)
    private String nome;

    @Column(name = "uf_sigla", nullable = false, length = 2, columnDefinition = "bpchar(2)")
    private String ufSigla;
}
