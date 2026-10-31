package br.com.superauto.crm.cadastro.entidade;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.compartilhado.entidade.DocumentoConverter;
import br.com.superauto.crm.compartilhado.entidade.Fila;
import br.com.superauto.crm.compartilhado.entidade.Telefone;

import org.hibernate.annotations.Immutable;

/**
 * Modelo de leitura da carteira (CAR-01): uma linha da visão {@code vw_carteira} por contrato, com a pessoa, o
 * veículo, a régua e a fila. Somente leitura; a escrita passa pelos agregados.
 */
@Entity
@Immutable
@Table(name = "vw_carteira")
public class LinhaCarteira {

    @Id
    @Column(name = "contrato_id")
    private Long contratoId;

    @Column(name = "contrato_versao")
    private int contratoVersao;

    @Column(name = "associado_id")
    private Long associadoId;

    @Column(name = "associado_nome")
    private String associadoNome;

    @Convert(converter = DocumentoConverter.class)
    private Documento documento;

    @Enumerated(EnumType.STRING)
    private Perfil perfil;

    @Enumerated(EnumType.STRING)
    @Column(name = "potencial_indicacao")
    private PotencialIndicacao potencialIndicacao;

    @Column(name = "municipio_id")
    private Integer municipioId;

    private String cidade;

    @Column(columnDefinition = "bpchar(2)")
    private String uf;

    @Convert(converter = PlacaConverter.class)
    private Placa placa;

    private String modelo;

    @Column(name = "tipo_veiculo_id")
    private Short tipoVeiculoId;

    @Column(name = "tipo_veiculo")
    private String tipoVeiculo;

    @Convert(converter = MensalidadeConverter.class)
    @Column(name = "valor_mensalidade")
    private Mensalidade mensalidade;

    @Enumerated(EnumType.STRING)
    private SituacaoContrato situacao;

    @Column(name = "codigo_sga")
    private String codigoSga;

    @Column(name = "aderido_em")
    private OffsetDateTime aderidoEm;

    @Column(name = "vendido_em")
    private OffsetDateTime vendidoEm;

    @Column(name = "encerrado_em")
    private OffsetDateTime encerradoEm;

    @Column(name = "vendedor_id")
    private UUID vendedorId;

    @Column(name = "vendedor_nome")
    private String vendedorNome;

    @Column(name = "etapa_pendente_id")
    private Short etapaPendenteId;

    @Column(name = "etapa_pendente")
    private String etapaPendente;

    @Column(name = "etapa_ordem")
    private Short etapaOrdem;

    @Column(name = "proximo_contato_em")
    private LocalDate proximoContatoEm;

    @Enumerated(EnumType.STRING)
    private Fila fila;

    @Column(name = "dias_atraso")
    private int diasAtraso;

    private String whatsapp;

    @Column(name = "whatsapp_aceita_contato")
    private Boolean whatsappAceitaContato;

    /** Todos os números do associado, só para a busca por telefone (RG-05). */
    private String telefones;

    protected LinhaCarteira() {
    }

    public Long getContratoId() {
        return contratoId;
    }

    public int getContratoVersao() {
        return contratoVersao;
    }

    public Long getAssociadoId() {
        return associadoId;
    }

    public String getAssociadoNome() {
        return associadoNome;
    }

    /** Vazio só em cadastro anonimizado. */
    public Optional<Documento> getDocumento() {
        return Optional.ofNullable(documento);
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public PotencialIndicacao getPotencialIndicacao() {
        return potencialIndicacao;
    }

    public Integer getMunicipioId() {
        return municipioId;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
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

    public String getTipoVeiculo() {
        return tipoVeiculo;
    }

    public Mensalidade getMensalidade() {
        return mensalidade;
    }

    public SituacaoContrato getSituacao() {
        return situacao;
    }

    public Optional<String> getCodigoSga() {
        return Optional.ofNullable(codigoSga);
    }

    public OffsetDateTime getAderidoEm() {
        return aderidoEm;
    }

    public Optional<OffsetDateTime> getVendidoEm() {
        return Optional.ofNullable(vendidoEm);
    }

    public Optional<OffsetDateTime> getEncerradoEm() {
        return Optional.ofNullable(encerradoEm);
    }

    public Optional<UUID> getVendedorId() {
        return Optional.ofNullable(vendedorId);
    }

    public Optional<String> getVendedorNome() {
        return Optional.ofNullable(vendedorNome);
    }

    public Optional<Short> getEtapaPendenteId() {
        return Optional.ofNullable(etapaPendenteId);
    }

    public Optional<String> getEtapaPendente() {
        return Optional.ofNullable(etapaPendente);
    }

    public Optional<LocalDate> getProximoContatoEm() {
        return Optional.ofNullable(proximoContatoEm);
    }

    public Fila getFila() {
        return fila;
    }

    public int getDiasAtraso() {
        return diasAtraso;
    }

    public Optional<Telefone> getWhatsapp() {
        return Optional.ofNullable(whatsapp).map(Telefone::new);
    }

    /** Falso quando o WhatsApp principal tem opt-out: o botão de conversa fica desativado (CAR-04). */
    public boolean whatsappAceitaContato() {
        return Boolean.TRUE.equals(whatsappAceitaContato);
    }
}
