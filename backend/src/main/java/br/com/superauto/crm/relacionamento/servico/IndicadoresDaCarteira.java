package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.relacionamento.entidade.SituacaoNaCarteira;
import br.com.superauto.crm.relacionamento.repositorio.ContagemDaCarteira;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** FIL-01: regras puras dos indicadores, calculadas a partir de contagens agregadas. */
public final class IndicadoresDaCarteira {
    public record Totais(
            long carteira,
            Map<String, Long> porTipo,
            BigDecimal mensalidadesAtivas,
            long aConferir,
            Map<String, Long> situacoes) {}

    public Totais consolidar(List<ContagemDaCarteira> contagens) {
        long carteira = 0, aConferir = 0;
        BigDecimal mensalidades = BigDecimal.ZERO;
        Map<String, Long> porTipo = new LinkedHashMap<>(), situacoes = new LinkedHashMap<>();
        for (var contagem : contagens) {
            situacoes.merge(contagem.situacao().name(), contagem.contratos(), Long::sum);
            if (contagem.situacao().pertenceACarteira()) {
                carteira += contagem.contratos();
                porTipo.merge(contagem.tipoVeiculo(), contagem.contratos(), Long::sum);
            }
            if (contagem.situacao() == SituacaoNaCarteira.ATIVO)
                mensalidades = mensalidades.add(contagem.mensalidades());
            if (contagem.situacao() == SituacaoNaCarteira.A_CONFERIR)
                aConferir += contagem.contratos();
        }
        return new Totais(
                carteira, Map.copyOf(porTipo), mensalidades, aConferir, Map.copyOf(situacoes));
    }
}
