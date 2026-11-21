package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;
import br.com.superauto.crm.assistente.entidade.RotaDaTarefa;
import br.com.superauto.crm.assistente.entidade.TarefaDeIa;

/** IA-05 · Uma tarefa do assistente e seus provedores, como a tela de configuração mostra. */
public record RotaConfigurada(TarefaDeIa tarefa, String nome, ProvedorDeIa principal, ProvedorDeIa reserva, int versao) {

    static RotaConfigurada de(RotaDaTarefa rota) {
        return new RotaConfigurada(rota.getTarefa(), rota.getTarefa().nome(), rota.getPrincipal(),
                rota.getReserva().orElse(null), rota.getVersao());
    }
}
