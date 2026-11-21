package br.com.superauto.crm.assistente.entidade;

import br.com.superauto.crm.erro.NegocioException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * IA-05 · Raiz do agregado: a configuração de um provedor de IA contratado (modelo, chave cifrada, ligado, limite
 * de gasto e preços para estimar o custo). Cada empresa que usa o CRM tem a sua.
 */
@Entity
@Table(name = "ia_provedor")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfiguracaoDoProvedor extends AbstractAggregateRoot<ConfiguracaoDoProvedor> {
    private static final BigDecimal UM_MILHAO = BigDecimal.valueOf(1_000_000);
    private static final int CARACTERES_EXIBIDOS_DA_CHAVE = 4;

    @Getter
    @Id
    @Enumerated(EnumType.STRING)
    private ProvedorDeIa provedor;

    private String modelo;

    @Column(name = "chave_cifrada")
    private String chaveCifrada;

    @Column(name = "chave_final")
    private String chaveFinal;

    @Getter
    private boolean ativo;

    @Getter
    @Column(name = "limite_mensal")
    private BigDecimal limiteMensal;

    @Column(name = "preco_entrada")
    private BigDecimal precoEntrada;

    @Column(name = "preco_saida")
    private BigDecimal precoSaida;

    @Getter
    @Version
    private int versao;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private OffsetDateTime atualizadoEm;

    /**
     * Aplica o ajuste do gerencial. A chave só muda quando uma nova é informada (já cifrada pelo cofre); ligar exige
     * modelo e chave.
     *
     * @param novaChave a chave nova, cifrada, e os últimos caracteres da original, para exibir
     */
    public void ajustar(AjusteDoProvedor ajuste, Optional<ChaveCifrada> novaChave, UUID autor) {
        if (versao != ajuste.versao()) throw NegocioException.edicaoSimultanea();
        var anterior = situacao();
        modelo = ajuste.modelo();
        novaChave.ifPresent(chave -> {
            chaveCifrada = chave.cifrada();
            chaveFinal = chave.final4();
        });
        ativo = ajuste.ativo();
        limiteMensal = ajuste.limiteMensal();
        precoEntrada = ajuste.precoEntrada().orElse(null);
        precoSaida = ajuste.precoSaida().orElse(null);
        if (ativo && !configurado())
            throw NegocioException.invalido("provedor-incompleto", "Informe o modelo e a chave antes de ligar o provedor.");
        registerEvent(new ConfiguracaoDeIaAlterada("ia_provedor", provedor.name(), anterior, situacao(), autor));
    }

    /** Ligado, com modelo e chave: pode receber chamadas. */
    public boolean prontoParaUso() {
        return ativo && configurado();
    }

    /** Ainda há limite no mês para mais chamadas (RG: limite de gasto mensal da IA-05). */
    public boolean dentroDoLimite(BigDecimal gastoDoMes) {
        return gastoDoMes.compareTo(limiteMensal) < 0;
    }

    /** Custo estimado da chamada; vazio quando os preços do provedor não foram informados. */
    public Optional<BigDecimal> custoDe(long tokensEntrada, long tokensSaida) {
        if (precoEntrada == null || precoSaida == null) return Optional.empty();
        BigDecimal custo = precoEntrada.multiply(BigDecimal.valueOf(tokensEntrada))
                .add(precoSaida.multiply(BigDecimal.valueOf(tokensSaida)))
                .divide(UM_MILHAO, 6, RoundingMode.HALF_UP);
        return Optional.of(custo);
    }

    private boolean configurado() {
        return modelo != null && chaveCifrada != null;
    }

    private Map<String, Object> situacao() {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("modelo", modelo);
        dados.put("ativo", ativo);
        dados.put("limiteMensal", limiteMensal == null ? null : limiteMensal.toPlainString());
        dados.put("precoEntrada", precoEntrada == null ? null : precoEntrada.toPlainString());
        dados.put("precoSaida", precoSaida == null ? null : precoSaida.toPlainString());
        dados.put("chaveFinal", chaveFinal);
        return dados;
    }

    public Optional<String> getModelo() {
        return Optional.ofNullable(modelo);
    }

    public Optional<String> getChaveCifrada() {
        return Optional.ofNullable(chaveCifrada);
    }

    public Optional<String> getChaveFinal() {
        return Optional.ofNullable(chaveFinal);
    }

    public Optional<BigDecimal> getPrecoEntrada() {
        return Optional.ofNullable(precoEntrada);
    }

    public Optional<BigDecimal> getPrecoSaida() {
        return Optional.ofNullable(precoSaida);
    }

    /** A chave já cifrada e os últimos caracteres da original. */
    public record ChaveCifrada(String cifrada, String final4) {
        public static String finalDe(String chave) {
            return chave.length() <= CARACTERES_EXIBIDOS_DA_CHAVE ? chave : chave.substring(chave.length() - CARACTERES_EXIBIDOS_DA_CHAVE);
        }
    }
}
