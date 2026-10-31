package br.com.superauto.crm.cadastro.entidade;

import java.util.Objects;

/** Perfil e potencial de indicação do associado (CAR-06). O cliente nunca vê essa classificação. */
public record Classificacao(Perfil perfil, PotencialIndicacao potencial) {

    public static final Classificacao PADRAO = new Classificacao(Perfil.REGULAR, PotencialIndicacao.NAO_AVALIADO);

    public Classificacao {
        Objects.requireNonNull(perfil, "perfil");
        Objects.requireNonNull(potencial, "potencial");
    }
}
