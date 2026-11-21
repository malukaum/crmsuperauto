package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;

import java.util.Objects;
import java.util.Optional;

/** IA-05 · O principal e o reserva escolhidos para uma tarefa, com a versão lida na tela (RG-06). */
public record DefinicaoDaRota(int versao, ProvedorDeIa principal, Optional<ProvedorDeIa> reserva) {

    public DefinicaoDaRota {
        Objects.requireNonNull(principal, "principal");
        Objects.requireNonNull(reserva, "reserva");
    }
}
