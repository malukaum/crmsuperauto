package br.com.superauto.crm.acesso.servico;

import java.util.Map;
import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.Conta;
import br.com.superauto.crm.acesso.repositorio.ProvedorIdentidade;
import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.seguranca.Papeis;

/**
 * Serviço de domínio: decide se a carteira de um vendedor pode ir para outro. Envolve duas contas e os
 * papéis delas, por isso não cabe em nenhuma. Só aceita como destino um vendedor ativo e diferente da origem.
 */
public class TransferenciaCarteira {

    private final ProvedorIdentidade identidade;

    public TransferenciaCarteira(ProvedorIdentidade identidade) {
        this.identidade = identidade;
    }

    /**
     * Vendedor com contratos vigentes só é desativado depois de transferi-los: sem destino, responde conflito
     * com a quantidade, para a tela perguntar para quem passar.
     *
     * @return o destino informado
     */
    public static UUID exigirDestinoInformado(UUID destino, long contratosVigentes) {
        if (destino == null) {
            throw NegocioException.conflito("contratos-a-transferir",
                    "Este usuário é vendedor de " + contratosVigentes
                            + (contratosVigentes == 1 ? " contrato vigente" : " contratos vigentes")
                            + ". Escolha outro vendedor para recebê-los antes de desativar.",
                    Map.of("contratosVigentes", contratosVigentes));
        }
        return destino;
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
