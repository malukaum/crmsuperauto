package br.com.superauto.crm.referencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Uma etapa da régua de pós-venda (RG-13): D+7 → D+30 → D+60 → D+90 → Trimestral, que aponta para si mesma.
 * O prazo conta a partir do contato anterior concluído, ou da adesão na etapa inicial. Só leitura até a
 * configuração dos prazos (FIL-06).
 */
@Entity
@Immutable
@Table(name = "etapa_relacionamento")
public class EtapaRelacionamento {

    @Id
    private Short id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 40)
    private String nome;

    @Column(nullable = false)
    private short ordem;

    @Column(name = "dias_apos_anterior", nullable = false)
    private short diasAposAnterior;

    @Column(name = "proxima_etapa_id")
    private Short proximaEtapaId;

    @Column(nullable = false)
    private boolean inicial;

    protected EtapaRelacionamento() {
    }

    public Short getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public short getOrdem() {
        return ordem;
    }

    public short getDiasAposAnterior() {
        return diasAposAnterior;
    }

    public Short getProximaEtapaId() {
        return proximaEtapaId;
    }

    public boolean isInicial() {
        return inicial;
    }
}
