package br.com.superauto.crm.acesso.servico;

import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.Conta;
import br.com.superauto.crm.acesso.repositorio.ProvedorIdentidade;
import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.seguranca.Papeis;

/**
 * Serviço de domínio: decide para quem vai a carteira de um vendedor desativado (ACE-04). Envolve duas contas e os
 * papéis delas, por isso não cabe em nenhuma. O destino escolhido precisa ser um vendedor ativo e diferente da
 * origem; sem escolha, a carteira fica com o gerencial que desativou, que depois a redistribui.
 */
public class TransferenciaCarteira {

    private final ProvedorIdentidade identidade;

    public TransferenciaCarteira(ProvedorIdentidade identidade) {
        this.identidade = identidade;
    }

    /** @return a conta do vendedor que vai receber os contratos */
    public Conta exigirVendedorDestino(UUID origem, UUID destino) {
        if (destino.equals(origem)) {
            throw invalida("Escolha um vendedor diferente do que será desativado.");
        }
        Conta conta = identidade.buscar(destino)
                .orElseThrow(() -> invalida("O vendedor escolhido para receber os contratos não existe."));
        if (!conta.ativo() || !identidade.papeis(destino).contains(Papeis.VENDEDOR)) {
            throw invalida("Os contratos só podem ser transferidos para um vendedor ativo.");
        }
        return conta;
    }

    private static NegocioException invalida(String mensagem) {
        return NegocioException.invalido("transferencia-invalida", mensagem);
    }
}
