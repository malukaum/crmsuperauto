package br.com.superauto.crm.acesso.infraestrutura;

import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.Conta;
import br.com.superauto.crm.acesso.repositorio.ProvedorIdentidade;
import br.com.superauto.crm.acesso.servico.ProjecaoUsuarioService;
import br.com.superauto.crm.cadastro.repositorio.VendedoresDaEquipe;
import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.seguranca.Papeis;

import org.springframework.stereotype.Component;

/**
 * O Acesso, que conhece contas e papéis, atende a porta {@link VendedoresDaEquipe} do Cadastro. O vendedor
 * escolhido pode nunca ter entrado no CRM: a projeção local é garantida aqui, porque {@code contrato.vendedor_id}
 * aponta para ela.
 */
@Component
public class VendedoresDaEquipeAcesso implements VendedoresDaEquipe {

    private final ProvedorIdentidade identidade;
    private final ProjecaoUsuarioService projecao;

    public VendedoresDaEquipeAcesso(ProvedorIdentidade identidade, ProjecaoUsuarioService projecao) {
        this.identidade = identidade;
        this.projecao = projecao;
    }

    @Override
    public void exigirVendedorAtivo(UUID vendedorId) {
        Conta conta = identidade.buscar(vendedorId)
                .filter(Conta::ativo)
                .filter(c -> identidade.papeis(vendedorId).contains(Papeis.VENDEDOR))
                .orElseThrow(() -> NegocioException.invalido("vendedor-invalido", "Escolha um vendedor ativo."));
        projecao.registrar(conta);
    }
}
