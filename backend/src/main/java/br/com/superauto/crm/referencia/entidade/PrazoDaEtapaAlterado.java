package br.com.superauto.crm.referencia.entidade;

import java.util.UUID;

/** Prazo alterado pelo gerencial, sem mudar a sequência da régua (FIL-06). */
public record PrazoDaEtapaAlterado(Short etapaId, int anterior, int novo, UUID autor) {}
