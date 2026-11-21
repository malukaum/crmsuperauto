package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.RotaDaTarefa;
import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;
import br.com.superauto.crm.assistente.entidade.ConfiguracaoDoProvedor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * IA-05 · Serviço de domínio: em que ordem tentar os provedores de uma tarefa. Primeiro o principal, depois o
 * reserva, pulando quem está desligado, sem modelo ou chave, ou com o limite do mês atingido.
 */
public final class SelecaoDeProvedores {

    /** Um provedor que pode ser chamado, e se ele é o reserva da tarefa. */
    public record Candidato(ConfiguracaoDoProvedor configuracao, boolean reserva) {}

    public List<Candidato> candidatos(RotaDaTarefa rota, Map<ProvedorDeIa, ConfiguracaoDoProvedor> configuracoes,
                                      Map<ProvedorDeIa, BigDecimal> gastoDoMes) {
        List<Candidato> candidatos = new ArrayList<>();
        var ordem = rota.ordemDeTentativa();
        for (int i = 0; i < ordem.size(); i++) {
            var provedor = ordem.get(i);
            boolean reserva = i > 0;
            Optional.ofNullable(configuracoes.get(provedor))
                    .filter(ConfiguracaoDoProvedor::prontoParaUso)
                    .filter(c -> c.dentroDoLimite(gastoDoMes.getOrDefault(provedor, BigDecimal.ZERO)))
                    .ifPresent(c -> candidatos.add(new Candidato(c, reserva)));
        }
        return List.copyOf(candidatos);
    }
}
