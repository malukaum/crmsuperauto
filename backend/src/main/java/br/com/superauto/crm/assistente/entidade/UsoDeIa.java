package br.com.superauto.crm.assistente.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * IA-05 / IA-06 · Uma chamada a um provedor de IA: quem, tarefa, tokens, custo, se deu certo e se foi o reserva.
 * Só inserção; nunca guarda o conteúdo enviado ou recebido.
 */
@Entity
@Table(name = "uso_ia")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsoDeIa {
    private static final int TAMANHO_DO_ERRO = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ProvedorDeIa provedor;

    private String modelo;

    @Enumerated(EnumType.STRING)
    private TarefaDeIa tarefa;

    @Column(name = "atendimento_id")
    private Long atendimentoId;

    @Column(name = "tokens_entrada")
    private long tokensEntrada;

    @Column(name = "tokens_saida")
    private long tokensSaida;

    private BigDecimal custo;

    private boolean sucesso;

    private boolean reserva;

    private String erro;

    @Column(name = "realizado_em")
    private OffsetDateTime realizadoEm;

    /** @param dados provedor, modelo, tarefa e se foi o reserva */
    public static UsoDeIa sucesso(Chamada dados, long tokensEntrada, long tokensSaida, BigDecimal custo) {
        var uso = novo(dados);
        uso.sucesso = true;
        uso.tokensEntrada = tokensEntrada;
        uso.tokensSaida = tokensSaida;
        uso.custo = custo;
        return uso;
    }

    public static UsoDeIa falha(Chamada dados, String motivo) {
        var uso = novo(dados);
        uso.erro = motivo == null ? null : motivo.substring(0, Math.min(motivo.length(), TAMANHO_DO_ERRO));
        return uso;
    }

    private static UsoDeIa novo(Chamada dados) {
        var uso = new UsoDeIa();
        uso.provedor = dados.provedor();
        uso.modelo = dados.modelo();
        uso.tarefa = dados.tarefa();
        uso.atendimentoId = dados.atendimentoId();
        uso.reserva = dados.reserva();
        uso.realizadoEm = dados.quando();
        return uso;
    }

    /** O que identifica uma chamada. */
    public record Chamada(ProvedorDeIa provedor, String modelo, TarefaDeIa tarefa, Long atendimentoId, boolean reserva,
                          OffsetDateTime quando) {
        public Chamada {
            Objects.requireNonNull(provedor, "provedor");
            Objects.requireNonNull(modelo, "modelo");
            Objects.requireNonNull(tarefa, "tarefa");
            Objects.requireNonNull(quando, "quando");
        }
    }
}
