package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.Tendencias;
import br.com.superauto.crm.assistente.entidade.SinalDoAtendimento;
import br.com.superauto.crm.assistente.entidade.Satisfacao;
import br.com.superauto.crm.assistente.entidade.ChanceDeIndicar;
import br.com.superauto.crm.assistente.entidade.Assunto;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** IA-03 · Serviço de domínio: soma os sinais dos resumos do período em indicadores. */
public final class CalculoDeTendencias {

    public Tendencias calcular(List<SinalDoAtendimento> sinais) {
        Map<Assunto, Long> porAssunto = new EnumMap<>(Assunto.class);
        Map<Satisfacao, Long> porSatisfacao = new EnumMap<>(Satisfacao.class);
        long risco = 0;
        long chanceAlta = 0;
        for (var sinal : sinais) {
            sinal.assuntos().forEach(nome -> porAssunto.merge(Assunto.valueOf(nome), 1L, Long::sum));
            if (sinal.satisfacao() != null) porSatisfacao.merge(sinal.satisfacao(), 1L, Long::sum);
            if (Boolean.TRUE.equals(sinal.riscoDeCancelamento())) risco++;
            if (sinal.chanceDeIndicar() == ChanceDeIndicar.ALTA) chanceAlta++;
        }
        return new Tendencias(sinais.size(), porAssunto, porSatisfacao, risco, chanceAlta);
    }
}
