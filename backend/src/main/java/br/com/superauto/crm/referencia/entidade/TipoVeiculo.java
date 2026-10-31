package br.com.superauto.crm.referencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Classificação do veículo protegido (carro, caminhão, moto). Desativado em vez de apagado; só os ativos
 * são oferecidos no cadastro. Ainda não há tela de manutenção, por isso é só leitura.
 */
@Entity
@Immutable
@Table(name = "tipo_veiculo")
public class TipoVeiculo {

    @Id
    private Short id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 80)
    private String descricao;

    @Column(nullable = false)
    private boolean ativo;

    protected TipoVeiculo() {
    }

    public Short getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
