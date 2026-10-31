package br.com.superauto.crm.acesso.servico;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import br.com.superauto.crm.acesso.entidade.Conta;
import br.com.superauto.crm.acesso.entidade.Usuario;
import br.com.superauto.crm.acesso.repositorio.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Mantém a projeção local ({@code usuario}) de cada pessoa da equipe. Na primeira vez a linha é criada;
 * depois só é tocada se username, nome ou e-mail mudarem (dirty checking do Hibernate). Um cache em memória
 * evita ir ao banco a cada requisição. Quem chama decide de onde vêm os dados (token ou provedor de identidade).
 *
 * <p>Roda numa transação própria ({@code REQUIRES_NEW}): a projeção espelha o Keycloak e não deve ser desfeita
 * nem derrubar a operação de quem chamou se falhar.
 */
@Service
public class ProjecaoUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(ProjecaoUsuarioService.class);

    private final UsuarioRepository repositorio;
    private final TransactionOperations transacao;
    private final Map<UUID, DadosProjecao> conhecidos = new ConcurrentHashMap<>();

    @Autowired
    public ProjecaoUsuarioService(UsuarioRepository repositorio, PlatformTransactionManager transacoes) {
        this(repositorio, novaTransacao(transacoes));
    }

    ProjecaoUsuarioService(UsuarioRepository repositorio, TransactionOperations transacao) {
        this.repositorio = repositorio;
        this.transacao = transacao;
    }

    public void registrar(Conta conta) {
        registrar(conta.id(), new DadosProjecao(conta.username(), conta.nomeCompleto(), conta.email()));
    }

    public void registrar(UUID id, DadosProjecao dados) {
        if (dados.equals(conhecidos.get(id))) {
            return;
        }
        try {
            boolean gravou = Boolean.TRUE.equals(transacao.execute(status -> gravar(id, dados)));
            if (gravou) {
                log.info("Projeção local do usuário {} ({}) gravada", dados.username(), id);
            }
            conhecidos.put(id, dados);
        } catch (DataAccessException e) {
            // Ex.: username já usado por outra conta (recriada no Keycloak com outro id). Não derruba a requisição.
            log.error("Não foi possível gravar a projeção local do usuário {} ({})", dados.username(), id, e);
        }
    }

    private boolean gravar(UUID id, DadosProjecao dados) {
        boolean mudou = repositorio.findById(id)
                .map(u -> u.atualizarDados(dados.username(), dados.nome(), dados.email()))
                .orElseGet(() -> {
                    repositorio.save(new Usuario(id, dados.username(), dados.nome(), dados.email()));
                    return true;
                });
        // Força o INSERT/UPDATE aqui dentro, para um erro de restrição cair no catch acima.
        repositorio.flush();
        return mudou;
    }

    private static TransactionOperations novaTransacao(PlatformTransactionManager transacoes) {
        var template = new TransactionTemplate(transacoes);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }
}
