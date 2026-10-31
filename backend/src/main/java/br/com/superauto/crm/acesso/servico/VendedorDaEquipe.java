package br.com.superauto.crm.acesso.servico;

import java.util.UUID;

/** Um vendedor ativo, para escolher como vendedor do contrato ou filtrar a carteira (CAR-01, CAR-02, CAR-05). */
public record VendedorDaEquipe(UUID id, String nome) {
}
