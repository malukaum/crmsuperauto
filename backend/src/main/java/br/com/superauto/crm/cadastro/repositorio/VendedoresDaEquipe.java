package br.com.superauto.crm.cadastro.repositorio;

import java.util.UUID;

/**
 * Quem pode ser vendedor de um contrato. Porta declarada pelo Cadastro e implementada pelo Acesso, que conhece
 * contas e papéis. A implementação também garante a projeção local do vendedor, alvo da chave estrangeira.
 */
public interface VendedoresDaEquipe {

    /** Lança {@code NegocioException} (inválido) se o id não for de um vendedor ativo. */
    void exigirVendedorAtivo(UUID vendedorId);
}
