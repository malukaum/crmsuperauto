package br.com.superauto.crm.acesso.servico;

import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.Conta;

/** Uma pessoa da equipe como a tela de gestão de usuários a mostra (ACE-04). */
public record UsuarioEquipe(UUID id, String username, String nome, String sobrenome, String email,
                            String papel, boolean ativo, boolean aguardandoSenha, long contratosVigentes) {

    static UsuarioEquipe de(Conta conta, String papel, long contratosVigentes) {
        String email = conta.email() == null ? null : conta.email().valor();
        return new UsuarioEquipe(conta.id(), conta.username(), conta.nome(), conta.sobrenome(), email,
                papel, conta.ativo(), conta.aguardandoSenha(), contratosVigentes);
    }
}
