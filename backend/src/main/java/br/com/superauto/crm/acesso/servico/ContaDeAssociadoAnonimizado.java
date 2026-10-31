package br.com.superauto.crm.acesso.servico;

import br.com.superauto.crm.acesso.repositorio.ProvedorIdentidade;
import br.com.superauto.crm.acesso.repositorio.UsuarioRepository;
import br.com.superauto.crm.cadastro.entidade.AssociadoAnonimizado;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * CAR-08 · Quando o associado é anonimizado, a conta dele na área do cliente é desativada e as sessões abertas caem.
 * Roda antes do commit: se o provedor de identidade falhar, a anonimização é desfeita.
 */
@Component
public class ContaDeAssociadoAnonimizado {

    private final ProvedorIdentidade identidade;
    private final UsuarioRepository usuarios;

    public ContaDeAssociadoAnonimizado(ProvedorIdentidade identidade, UsuarioRepository usuarios) {
        this.identidade = identidade;
        this.usuarios = usuarios;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoAnonimizar(AssociadoAnonimizado evento) {
        if (evento.usuarioId() == null) {
            return;
        }
        usuarios.findById(evento.usuarioId()).ifPresent(usuario -> usuario.definirAtivo(false));
        identidade.definirAtivo(evento.usuarioId(), false);
        identidade.encerrarSessoes(evento.usuarioId());
    }
}
