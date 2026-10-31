package br.com.superauto.crm.cadastro.servico;

import java.util.Objects;

import br.com.superauto.crm.cadastro.repositorio.OrdemCarteira;

/** Coluna e direção da ordenação da carteira (CAR-01). O padrão é por nome, crescente. */
public record Ordenacao(OrdemCarteira coluna, boolean decrescente) {

    public static final Ordenacao PADRAO = new Ordenacao(OrdemCarteira.NOME, false);

    public Ordenacao {
        Objects.requireNonNull(coluna, "coluna");
    }
}
