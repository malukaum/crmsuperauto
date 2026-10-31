package br.com.superauto.crm.relacionamento.entidade;

import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.compartilhado.entidade.DocumentoConverter;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import br.com.superauto.crm.compartilhado.entidade.MensalidadeConverter;
import br.com.superauto.crm.compartilhado.entidade.Placa;
import br.com.superauto.crm.compartilhado.entidade.PlacaConverter;
import br.com.superauto.crm.compartilhado.entidade.Telefone;
import br.com.superauto.crm.compartilhado.entidade.TelefoneConverter;
import br.com.superauto.crm.erro.NegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.util.UUID;

/** Leitura do trabalho da equipe, uma linha por contrato, sem dados de outros vendedores. */
@Entity
@Immutable
@Table(name = "vw_trabalho_relacionamento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContratoNaFila {
    @Id
    @Column(name = "contrato_id")
    private Long contratoId;

    @Column(name = "associado_id")
    private Long associadoId;

    @Column(name = "associado_nome")
    private String nome;

    @Column(name = "documento")
    @Convert(converter = DocumentoConverter.class)
    private Documento documento;

    @Column(name = "telefones")
    private String telefones;

    @Column(name = "cidade")
    private String cidade;

    @Column(name = "municipio_id")
    private Integer municipioId;

    @Column(name = "placa")
    @Convert(converter = PlacaConverter.class)
    private Placa placa;

    @Column(name = "modelo")
    private String modelo;

    @Column(name = "tipo_veiculo")
    private String tipoVeiculo;

    @Column(name = "tipo_veiculo_id")
    private Short tipoVeiculoId;

    @Column(name = "valor_mensalidade")
    @Convert(converter = MensalidadeConverter.class)
    private Mensalidade mensalidade;

    @Column(name = "situacao")
    @Enumerated(EnumType.STRING)
    private SituacaoNaCarteira situacao;

    @Column(name = "vendedor_id")
    private UUID vendedorId;

    @Column(name = "etapa_pendente_id")
    private Short etapaId;

    @Column(name = "etapa_pendente")
    private String etapa;

    @Column(name = "proximo_contato_em")
    private LocalDate proximoContato;

    @Column(name = "whatsapp")
    @Convert(converter = TelefoneConverter.class)
    private Telefone whatsapp;

    @Column(name = "whatsapp_aceita_contato")
    private Boolean aceitaWhatsapp;

    @Column(name = "data_nascimento")
    private LocalDate nascimento;

    @Column(name = "base_regua")
    private LocalDate baseRegua;

    @Column(name = "acompanhamento_versao")
    private int versao;

    public boolean permiteContato() {
        return situacao.permiteContato();
    }

    public void exigirPosVendaDisponivel() {
        if (!permiteContato())
            throw NegocioException.conflito(
                    "fila-mudou", "A fila mudou. Recarregue para ver a situação atual.");
    }

    public void exigirAniversarioDisponivel() {
        if (!permiteContato() || nascimento == null)
            throw NegocioException.invalido(
                    "aniversario-indisponivel",
                    "O associado não participa da fila de aniversariantes.");
    }
}
