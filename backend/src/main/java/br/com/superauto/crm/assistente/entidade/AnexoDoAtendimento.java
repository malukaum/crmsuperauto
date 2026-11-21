package br.com.superauto.crm.assistente.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Parte do agregado {@link AnotacaoDoAtendimento}: um anexo. O conteúdo existe só até o resumo ficar pronto (IA-02);
 * depois ficam o tipo, o tamanho e a impressão digital (hash) do arquivo.
 */
@Entity
@Table(name = "anexo_atendimento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnexoDoAtendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(name = "tipo_conteudo")
    private String tipoConteudo;

    private long tamanho;

    private String hash;

    private byte[] conteudo;

    @Column(name = "descartado_em")
    private OffsetDateTime descartadoEm;

    static AnexoDoAtendimento de(NovoAnexo novo) {
        var anexo = new AnexoDoAtendimento();
        anexo.nome = novo.nome();
        anexo.tipoConteudo = novo.tipoConteudo();
        anexo.tamanho = novo.conteudo().length;
        anexo.hash = sha256(novo.conteudo());
        anexo.conteudo = novo.conteudo().clone();
        return anexo;
    }

    void descartarConteudo(OffsetDateTime quando) {
        if (conteudo != null) {
            conteudo = null;
            descartadoEm = quando;
        }
    }

    public Optional<byte[]> getConteudo() {
        return Optional.ofNullable(conteudo).map(byte[]::clone);
    }

    public boolean ehTexto() {
        return "text/plain".equals(tipoConteudo);
    }

    public boolean ehPdf() {
        return "application/pdf".equals(tipoConteudo);
    }

    private static String sha256(byte[] dados) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dados));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Usado só para texto que já é texto (anexo .txt). */
    public Optional<String> textoSimples() {
        return ehTexto() ? getConteudo().map(bytes -> new String(bytes, StandardCharsets.UTF_8)) : Optional.empty();
    }
}
