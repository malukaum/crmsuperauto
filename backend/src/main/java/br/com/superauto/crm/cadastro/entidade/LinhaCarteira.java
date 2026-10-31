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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.Immutable;

/**
 * Modelo de leitura da carteira (CAR-01): uma linha da visão {@code vw_carteira} por contrato, com a pessoa, o
 * veículo, a régua e a fila. Somente leitura; a escrita passa pelos agregados.
 */
@Entity
@Immutable
@Table(name = "vw_carteira")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LinhaCarteira {

    @Getter
    @Id
    @Column(name = "contrato_id")
    private Long contratoId;

    @Getter
    @Column(name = "contrato_versao")
    private int contratoVersao;

    @Getter
    @Column(name = "associado_id")
    private Long associadoId;

    @Getter
    @Column(name = "associado_nome")
    private String associadoNome;

    @Convert(converter = DocumentoConverter.class)
    private Documento documento;

    @Getter
    @Enumerated(EnumType.STRING)
    private Perfil perfil;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(name = "potencial_indicacao")
    private PotencialIndicacao potencialIndicacao;

    @Getter
    @Column(name = "municipio_id")
    private Integer municipioId;

    @Getter
    private String cidade;

    @Getter
    @Column(columnDefinition = "bpchar(2)")
    private String uf;

    @Getter
    @Convert(converter = PlacaConverter.class)
    private Placa placa;

    @Getter
    private String modelo;

    @Getter
    @Column(name = "tipo_veiculo_id")
    private Short tipoVeiculoId;

    @Getter
    @Column(name = "tipo_veiculo")
    private String tipoVeiculo;

    @Getter
    @Convert(converter = MensalidadeConverter.class)
    @Column(name = "valor_mensalidade")
    private Mensalidade mensalidade;

    @Getter
    @Enumerated(EnumType.STRING)
    private SituacaoContrato situacao;

    @Column(name = "codigo_sga")
    private String codigoSga;

    @Getter
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

    @Getter
    @Enumerated(EnumType.STRING)
    private Fila fila;

    @Getter
    @Column(name = "dias_atraso")
    private int diasAtraso;

    private String whatsapp;

    @Column(name = "whatsapp_aceita_contato")
    private Boolean whatsappAceitaContato;

    /** Todos os números do associado, só para a busca por telefone (RG-05). */
    private String telefones;

    /** Vazio só em cadastro anonimizado. */
    public Optional<Documento> getDocumento() {
        return Optional.ofNullable(documento);
    }

    public Optional<String> getCodigoSga() {
        return Optional.ofNullable(codigoSga);
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

    public Optional<Telefone> getWhatsapp() {
        return Optional.ofNullable(whatsapp).map(Telefone::new);
    }

    /** Falso quando o WhatsApp principal tem opt-out: o botão de conversa fica desativado (CAR-04). */
    public boolean whatsappAceitaContato() {
        return Boolean.TRUE.equals(whatsappAceitaContato);
    }
}
