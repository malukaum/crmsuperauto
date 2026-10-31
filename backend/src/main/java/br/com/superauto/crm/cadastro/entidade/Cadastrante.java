package br.com.superauto.crm.cadastro.entidade;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import br.com.superauto.crm.erro.NegocioException;

/**
 * Quem está cadastrando o contrato (CAR-02). O vendedor do contrato é quem cadastra; o gerencial escolhe o vendedor.
 *
 * @param gerencial se o autor tem o papel gerencial
 */
public record Cadastrante(UUID id, boolean gerencial) {

    public Cadastrante {
        Objects.requireNonNull(id, "id");
    }

    /** @param escolhido o vendedor indicado na tela; só vale (e é obrigatório) para o gerencial */
    public UUID vendedorDoContrato(Optional<UUID> escolhido) {
        if (!gerencial) {
            return id;
        }
        return escolhido.orElseThrow(() -> NegocioException.invalido("vendedor-obrigatorio",
                "Escolha o vendedor do contrato."));
    }
}
