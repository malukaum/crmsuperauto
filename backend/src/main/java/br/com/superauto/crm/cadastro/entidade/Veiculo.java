package br.com.superauto.crm.cadastro.entidade;

import java.time.OffsetDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import br.com.superauto.crm.erro.NegocioException;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Raiz do agregado Veículo: identificado pela placa, independente de quem o protege. Um veículo pode ter vários
 * contratos ao longo do tempo, mas só um vigente por vez ({@code uk_contrato_veiculo_vigente}).
 */
@Entity
@Table(name = "veiculo")
public class Veiculo {

    static final int TAMANHO_MAXIMO_MODELO = 150;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = PlacaConverter.class)
    @Column(nullable = false, length = 7, unique = true)
    private Placa placa;

    @Column(nullable = false, length = TAMANHO_MAXIMO_MODELO)
    private String modelo;

    /** Tipo de veículo (referência): só o id. */
    @Column(name = "tipo_veiculo_id", nullable = false)
    private Short tipoVeiculoId;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    @Version
    @Column(nullable = false)
    private int versao;

    protected Veiculo() {
    }

    public static Veiculo cadastrar(Placa placa, String modelo, Short tipoVeiculoId) {
        var veiculo = new Veiculo();
        veiculo.placa = Objects.requireNonNull(placa, "placa");
        veiculo.descrever(modelo, tipoVeiculoId);
        return veiculo;
    }

    /** Modelo e tipo informados num novo contrato da mesma placa valem a partir de agora. */
    public void descrever(String novoModelo, Short novoTipoVeiculoId) {
        String normalizado = Objects.requireNonNullElse(novoModelo, "").strip();
        if (normalizado.isEmpty() || normalizado.length() > TAMANHO_MAXIMO_MODELO) {
            throw NegocioException.invalido("modelo-invalido", "Informe o modelo do veículo, com até 150 caracteres.");
        }
        this.modelo = normalizado;
        this.tipoVeiculoId = Objects.requireNonNull(novoTipoVeiculoId, "tipoVeiculoId");
    }

    public Long getId() {
        return id;
    }

    public Placa getPlaca() {
        return placa;
    }

    public String getModelo() {
        return modelo;
    }

    public Short getTipoVeiculoId() {
        return tipoVeiculoId;
    }

    public int getVersao() {
        return versao;
    }
}
