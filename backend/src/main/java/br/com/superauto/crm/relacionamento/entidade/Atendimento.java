package br.com.superauto.crm.relacionamento.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Raiz do agregado Atendimento: um contato com o associado. É imutável (RG-07): a correção é um novo registro
 * (ATD-04). Por enquanto só o que a anonimização (CAR-08) precisa está mapeado; o registro chega com a ATD-01.
 */
@Entity
@Table(name = "atendimento")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Atendimento {

    @Getter
    @Id
    private Long id;

    @Getter
    @Column(name = "associado_id", nullable = false, updatable = false)
    private Long associadoId;

    @Column(length = 2000)
    private String descricao;

    /**
     * CAR-08 · A única mudança permitida num atendimento: a descrição pode conter dados pessoais e é apagada
     * quando o associado é anonimizado (proposta). Data, tipo, resultado e autor continuam, para os relatórios.
     */
    public void apagarDescricaoPorAnonimizacao() {
        descricao = null;
    }
}
