package br.com.superauto.crm.acesso.servico;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.Conta;
import br.com.superauto.crm.acesso.entidade.PapelAlterado;
import br.com.superauto.crm.acesso.entidade.UsuarioCriado;
import br.com.superauto.crm.acesso.entidade.UsuarioDesativado;
import br.com.superauto.crm.acesso.entidade.UsuarioReativado;
import br.com.superauto.crm.acesso.repositorio.ContratosDoVendedor;
import br.com.superauto.crm.acesso.repositorio.ProvedorIdentidade;
import br.com.superauto.crm.acesso.repositorio.ProvedorIdentidade.NovaConta;
import br.com.superauto.crm.acesso.repositorio.UsuarioRepository;
import br.com.superauto.crm.compartilhado.entidade.Email;
import br.com.superauto.crm.erro.IntegracaoIndisponivelException;
import br.com.superauto.crm.erro.NegocioException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço de aplicação com os comandos da gestão dos usuários da equipe (ACE-04); a listagem está em
 * {@link ConsultaEquipeService}. A conta e o papel vivem no
 * {@link ProvedorIdentidade}; aqui ficam a orquestração dos casos de uso e a projeção local. As regras ficam no
 * domínio ({@link RegrasEquipe}, {@link TransferenciaCarteira}) e cada mudança sai como evento de domínio,
 * que a auditoria registra (RG-08).
 */
@Service
public class GestaoUsuariosService {

    private static final Logger log = LoggerFactory.getLogger(GestaoUsuariosService.class);

    private final ProvedorIdentidade identidade;
    private final ProjecaoUsuarioService projecao;
    private final UsuarioRepository usuarios;
    private final ContratosDoVendedor contratos;
    private final TransferenciaCarteira transferencia;
    private final ApplicationEventPublisher eventos;

    public GestaoUsuariosService(ProvedorIdentidade identidade, ProjecaoUsuarioService projecao,
                                 UsuarioRepository usuarios, ContratosDoVendedor contratos,
                                 TransferenciaCarteira transferencia, ApplicationEventPublisher eventos) {
        this.identidade = identidade;
        this.projecao = projecao;
        this.usuarios = usuarios;
        this.contratos = contratos;
        this.transferencia = transferencia;
        this.eventos = eventos;
    }

    public ResultadoCriacaoUsuario criar(NovoUsuario novo, UUID autor) {
        String papel = RegrasEquipe.exigirPapelDaEquipe(novo.papel());
        var conta = new NovaConta(novo.username().trim().toLowerCase(Locale.ROOT), novo.nome().trim(),
                novo.sobrenome().trim(), Email.de(novo.email()));

        UUID id = identidade.criar(conta);
        identidade.adicionarPapeis(id, List.of(papel));
        var criada = new Conta(id, conta.username(), conta.nome(), conta.sobrenome(), conta.email(), true, true);
        projecao.registrar(criada);
        eventos.publishEvent(new UsuarioCriado(id, criada.username(), criada.nomeCompleto(), criada.email(), papel, autor));

        boolean emailEnviado = enviarConvite(id);
        log.info("Usuário {} criado com o papel {} (e-mail enviado: {})", criada.username(), papel, emailEnviado);
        return new ResultadoCriacaoUsuario(UsuarioEquipe.de(criada, papel, 0), emailEnviado);
    }

    public UsuarioEquipe alterarPapel(UUID id, String novoPapel, UUID autor) {
        String papel = RegrasEquipe.exigirPapelDaEquipe(novoPapel);
        exigirOutraPessoa(id, autor, "Você não pode trocar o seu próprio papel.");
        Conta conta = buscarDaEquipe(id);
        List<String> atuais = identidade.papeis(id);
        String anterior = papelDaEquipe(atuais);
        if (RegrasEquipe.precisaTrocarPapel(atuais, papel)) {
            trocarPapel(conta, atuais, papel);
            eventos.publishEvent(new PapelAlterado(id, anterior, papel, autor));
        }
        return UsuarioEquipe.de(conta, papel, contratos.contar(id));
    }

    /**
     * Desativa o login. Se a pessoa tiver contratos vigentes como vendedor, exige {@code transferirPara}
     * (outro vendedor ativo); sem isso responde conflito com a quantidade, para a tela perguntar.
     */
    @Transactional
    public UsuarioEquipe desativar(UUID id, UUID transferirPara, UUID autor) {
        exigirOutraPessoa(id, autor, "Você não pode desativar o seu próprio usuário.");
        Conta conta = buscarDaEquipe(id);
        String papel = papelDaEquipe(identidade.papeis(id));
        long vigentes = contratos.contar(id);
        int transferidos = vigentes > 0
                ? transferirCarteira(id, TransferenciaCarteira.exigirDestinoInformado(transferirPara, vigentes), autor)
                : 0;
        projecao.registrar(conta);
        definirAtivoLocal(id, false);
        eventos.publishEvent(new UsuarioDesativado(id, conta.ativo(), transferidos,
                transferidos > 0 ? transferirPara : null, autor));

        // Por último o provedor: se ele falhar, a transação desfaz a transferência, a desativação local e a auditoria.
        identidade.definirAtivo(id, false);
        identidade.encerrarSessoes(id);
        log.info("Usuário {} desativado ({} contratos transferidos)", conta.username(), transferidos);
        return UsuarioEquipe.de(conta.comAtivo(false), papel, 0);
    }

    @Transactional
    public UsuarioEquipe reativar(UUID id, UUID autor) {
        Conta conta = buscarDaEquipe(id);
        String papel = papelDaEquipe(identidade.papeis(id));
        projecao.registrar(conta);
        definirAtivoLocal(id, true);
        eventos.publishEvent(new UsuarioReativado(id, conta.ativo(), autor));
        identidade.definirAtivo(id, true);
        return UsuarioEquipe.de(conta.comAtivo(true), papel, contratos.contar(id));
    }

    /** Reenvia o e-mail para definir a senha (ex.: o link expirou ou o e-mail não chegou). */
    public void reenviarConvite(UUID id) {
        Conta conta = buscarDaEquipe(id);
        if (!conta.ativo()) {
            throw NegocioException.invalido("usuario-inativo", "Reative o usuário antes de reenviar o convite.");
        }
        if (!enviarConvite(id)) {
            throw new IntegracaoIndisponivelException(
                    "Não foi possível enviar o e-mail agora. Verifique o servidor de e-mail e tente novamente.", null);
        }
    }

    // ---------- apoio ----------

    private void trocarPapel(Conta conta, List<String> atuais, String papel) {
        if (!atuais.contains(papel)) {
            identidade.adicionarPapeis(conta.id(), List.of(papel));
        }
        identidade.removerPapeis(conta.id(), RegrasEquipe.papeisASubstituir(atuais, papel));
        // O token atual ainda traz o papel antigo: força um novo login para valer já.
        identidade.encerrarSessoes(conta.id());
        projecao.registrar(conta);
    }

    private int transferirCarteira(UUID vendedor, UUID destino, UUID autor) {
        Conta contaDestino = transferencia.exigirVendedorDestino(vendedor, destino);
        // O vendedor de destino pode nunca ter entrado no CRM: garante a projeção (FK de contrato.vendedor_id).
        projecao.registrar(contaDestino);
        return contratos.transferir(vendedor, destino, autor);
    }

    private static void exigirOutraPessoa(UUID alvo, UUID autor, String mensagem) {
        if (alvo.equals(autor)) {
            throw NegocioException.invalido("proprio-usuario", mensagem);
        }
    }

    /** Altera a projeção carregada na transação; o Hibernate grava o UPDATE no commit. */
    private void definirAtivoLocal(UUID id, boolean ativo) {
        usuarios.findById(id).ifPresent(u -> u.definirAtivo(ativo));
    }

    private Conta buscarDaEquipe(UUID id) {
        return identidade.buscar(id)
                .filter(c -> !RegrasEquipe.ehContaTecnica(c))
                .orElseThrow(() -> NegocioException.naoEncontrado("Usuário não encontrado."));
    }

    /** O papel da equipe da conta; contas de cliente (ou sem papel) não são geridas por aqui. */
    private static String papelDaEquipe(List<String> papeis) {
        return RegrasEquipe.papelPrincipal(papeis).orElseThrow(
                () -> NegocioException.naoEncontrado("Usuário não encontrado entre os usuários da equipe."));
    }

    private boolean enviarConvite(UUID id) {
        try {
            identidade.enviarConviteDefinirSenha(id);
            return true;
        } catch (RuntimeException e) {
            log.warn("Falha ao enviar o e-mail de definição de senha para o usuário {}", id, e);
            return false;
        }
    }
}
