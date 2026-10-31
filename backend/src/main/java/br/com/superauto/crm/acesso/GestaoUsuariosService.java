package br.com.superauto.crm.acesso;

import java.text.Collator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import br.com.superauto.crm.acesso.KeycloakAdminClient.UsuarioKeycloak;
import br.com.superauto.crm.auditoria.RegistroAuditoria;
import br.com.superauto.crm.erro.IntegracaoIndisponivelException;
import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.seguranca.Papeis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão dos usuários da equipe (ACE-04). A conta e o papel vivem no Keycloak; aqui ficam a projeção
 * local, a auditoria e as regras: só papéis da equipe, ninguém é excluído (só desativado), e um vendedor
 * com contratos vigentes só é desativado depois de transferi-los a outro vendedor.
 */
@Service
public class GestaoUsuariosService {

    private static final Logger log = LoggerFactory.getLogger(GestaoUsuariosService.class);
    private static final String ENTIDADE = "usuario";
    private static final String ACAO_DEFINIR_SENHA = "UPDATE_PASSWORD";

    private final KeycloakAdminClient keycloak;
    private final ProjecaoUsuarioService projecao;
    private final UsuarioRepository usuarios;
    private final ContratosDoVendedor contratos;
    private final RegistroAuditoria auditoria;

    public GestaoUsuariosService(KeycloakAdminClient keycloak, ProjecaoUsuarioService projecao,
                                 UsuarioRepository usuarios, ContratosDoVendedor contratos,
                                 RegistroAuditoria auditoria) {
        this.keycloak = keycloak;
        this.projecao = projecao;
        this.usuarios = usuarios;
        this.contratos = contratos;
        this.auditoria = auditoria;
    }

    public record UsuarioEquipe(UUID id, String username, String nome, String sobrenome, String email,
                                String papel, boolean ativo, boolean aguardandoSenha, long contratosVigentes) {
    }

    public record NovoUsuario(String username, String nome, String sobrenome, String email, String papel) {
    }

    public record UsuarioCriado(UsuarioEquipe usuario, boolean emailEnviado) {
    }

    /** Todos com papel da equipe no Keycloak, ativos e desativados. Clientes e o usuário técnico ficam de fora. */
    public List<UsuarioEquipe> listar() {
        Map<String, UsuarioKeycloak> porId = new LinkedHashMap<>();
        Map<String, String> papelPorId = new LinkedHashMap<>();
        // Papeis.EQUIPE está do mais amplo para o mais restrito: quem tiver dois fica com o mais amplo.
        for (String papel : Papeis.EQUIPE) {
            for (UsuarioKeycloak u : keycloak.usuariosComPapel(papel)) {
                porId.putIfAbsent(u.id(), u);
                papelPorId.putIfAbsent(u.id(), papel);
            }
        }
        Map<UUID, Long> contagem = contratos.contarPorVendedor();
        Collator collator = Collator.getInstance(Locale.of("pt", "BR"));
        return porId.values().stream()
                .filter(u -> !ehUsuarioTecnico(u))
                .map(u -> paraDto(u, papelPorId.get(u.id()), contagem.getOrDefault(UUID.fromString(u.id()), 0L)))
                .sorted(Comparator.comparing(UsuarioEquipe::ativo).reversed()
                        .thenComparing((UsuarioEquipe u) -> nomeCompleto(u), collator))
                .toList();
    }

    public UsuarioCriado criar(NovoUsuario novo, UUID autor) {
        String papel = exigirPapelDaEquipe(novo.papel());
        String username = novo.username().trim().toLowerCase(Locale.ROOT);
        String email = novo.email().trim().toLowerCase(Locale.ROOT);
        var rep = new UsuarioKeycloak(null, username, novo.nome().trim(), novo.sobrenome().trim(), email,
                true, false, List.of(ACAO_DEFINIR_SENHA));

        String id = keycloak.criar(rep);
        keycloak.adicionarPapeis(id, List.of(papel));
        UUID uuid = UUID.fromString(id);
        projecao.registrar(uuid, username, rep.nomeCompleto(), email);
        auditoria.registrar(ENTIDADE, id, RegistroAuditoria.CRIACAO, autor, null,
                Map.of("username", username, "nome", rep.nomeCompleto(), "email", email, "papel", papel, "ativo", true));

        boolean emailEnviado = enviarConvite(id);
        log.info("Usuário {} criado com o papel {} (e-mail enviado: {})", username, papel, emailEnviado);
        var criado = new UsuarioKeycloak(id, username, rep.firstName(), rep.lastName(), email, true, false,
                List.of(ACAO_DEFINIR_SENHA));
        return new UsuarioCriado(paraDto(criado, papel, 0), emailEnviado);
    }

    public UsuarioEquipe alterarPapel(UUID id, String novoPapel, UUID autor) {
        String papel = exigirPapelDaEquipe(novoPapel);
        if (id.equals(autor)) {
            throw NegocioException.invalido("proprio-usuario", "Você não pode trocar o seu próprio papel.");
        }
        UsuarioKeycloak u = buscarDaEquipe(id);
        List<String> doRealm = keycloak.papeisDoRealm(id.toString());
        String atual = papelDaEquipe(doRealm);
        if (!papel.equals(atual) || doRealm.stream().filter(Papeis.EQUIPE::contains).count() > 1) {
            if (!doRealm.contains(papel)) {
                keycloak.adicionarPapeis(id.toString(), List.of(papel));
            }
            // Cada pessoa da equipe tem um único papel: tira os demais.
            keycloak.removerPapeis(id.toString(), Papeis.EQUIPE.stream()
                    .filter(p -> !p.equals(papel) && doRealm.contains(p))
                    .toList());
            // O token atual ainda traz o papel antigo: força um novo login para valer já.
            keycloak.encerrarSessoes(id.toString());
            projecao.registrar(id, u.username(), u.nomeCompleto(), u.email());
            auditoria.registrar(ENTIDADE, id.toString(), RegistroAuditoria.ALTERACAO, autor,
                    Map.of("papel", atual), Map.of("papel", papel));
        }
        return paraDto(u, papel, contratos.contar(id));
    }

    /**
     * Desativa o login. Se a pessoa tiver contratos vigentes como vendedor, exige {@code transferirPara}
     * (outro vendedor ativo); sem isso responde 409 com a quantidade, para a tela perguntar.
     */
    @Transactional
    public UsuarioEquipe desativar(UUID id, UUID transferirPara, UUID autor) {
        if (id.equals(autor)) {
            throw NegocioException.invalido("proprio-usuario", "Você não pode desativar o seu próprio usuário.");
        }
        UsuarioKeycloak u = buscarDaEquipe(id);
        String papel = papelDaEquipe(id);
        long vigentes = contratos.contar(id);
        int transferidos = 0;
        if (vigentes > 0) {
            if (transferirPara == null) {
                throw new NegocioException(HttpStatus.CONFLICT, "contratos-a-transferir",
                        "Este usuário é vendedor de " + vigentes + (vigentes == 1 ? " contrato vigente" : " contratos vigentes")
                                + ". Escolha outro vendedor para recebê-los antes de desativar.",
                        Map.of("contratosVigentes", vigentes));
            }
            transferidos = transferirContratos(id, transferirPara, autor);
        }
        projecao.registrar(id, u.username(), u.nomeCompleto(), u.email());
        usuarios.definirAtivo(id, false);
        Map<String, Object> novo = new LinkedHashMap<>();
        novo.put("ativo", false);
        if (transferidos > 0) {
            novo.put("contratosTransferidos", transferidos);
            novo.put("transferidosPara", transferirPara.toString());
        }
        auditoria.registrar(ENTIDADE, id.toString(), RegistroAuditoria.ALTERACAO, autor, Map.of("ativo", u.ativo()), novo);

        // Por último o Keycloak: se ele falhar, a transação desfaz a transferência e a desativação local.
        keycloak.definirAtivo(id.toString(), false);
        keycloak.encerrarSessoes(id.toString());
        log.info("Usuário {} desativado ({} contratos transferidos)", u.username(), transferidos);
        return paraDto(desativado(u), papel, 0);
    }

    @Transactional
    public UsuarioEquipe reativar(UUID id, UUID autor) {
        UsuarioKeycloak u = buscarDaEquipe(id);
        String papel = papelDaEquipe(id);
        projecao.registrar(id, u.username(), u.nomeCompleto(), u.email());
        usuarios.definirAtivo(id, true);
        auditoria.registrar(ENTIDADE, id.toString(), RegistroAuditoria.ALTERACAO, autor,
                Map.of("ativo", u.ativo()), Map.of("ativo", true));
        keycloak.definirAtivo(id.toString(), true);
        var ativo = new UsuarioKeycloak(u.id(), u.username(), u.firstName(), u.lastName(), u.email(), true,
                u.emailVerified(), u.requiredActions());
        return paraDto(ativo, papel, contratos.contar(id));
    }

    /** Reenvia o e-mail para definir a senha (ex.: o link expirou ou o e-mail não chegou). */
    public void reenviarConvite(UUID id) {
        UsuarioKeycloak u = buscarDaEquipe(id);
        if (!u.ativo()) {
            throw NegocioException.invalido("usuario-inativo", "Reative o usuário antes de reenviar o convite.");
        }
        if (!enviarConvite(id.toString())) {
            throw new IntegracaoIndisponivelException(
                    "Não foi possível enviar o e-mail agora. Verifique o servidor de e-mail e tente novamente.", null);
        }
    }

    // ---------- apoio ----------

    private int transferirContratos(UUID de, UUID para, UUID autor) {
        if (para.equals(de)) {
            throw NegocioException.invalido("transferencia-invalida", "Escolha um vendedor diferente do que será desativado.");
        }
        UsuarioKeycloak destino;
        try {
            destino = keycloak.buscar(para.toString());
        } catch (NegocioException e) {
            throw NegocioException.invalido("transferencia-invalida", "O vendedor escolhido para receber os contratos não existe.");
        }
        if (!destino.ativo() || !keycloak.papeisDoRealm(para.toString()).contains(Papeis.VENDEDOR)) {
            throw NegocioException.invalido("transferencia-invalida",
                    "Os contratos só podem ser transferidos para um vendedor ativo.");
        }
        // O vendedor de destino pode nunca ter entrado no CRM: garante a projeção (FK de contrato.vendedor_id).
        projecao.registrar(para, destino.username(), destino.nomeCompleto(), destino.email());
        return contratos.transferir(de, para, autor);
    }

    private UsuarioKeycloak buscarDaEquipe(UUID id) {
        UsuarioKeycloak u = keycloak.buscar(id.toString());
        if (u == null || ehUsuarioTecnico(u)) {
            throw NegocioException.naoEncontrado("Usuário não encontrado.");
        }
        return u;
    }

    /** O papel da equipe do usuário; contas de cliente (ou sem papel) não são geridas por aqui. */
    private String papelDaEquipe(UUID id) {
        return papelDaEquipe(keycloak.papeisDoRealm(id.toString()));
    }

    private static String papelDaEquipe(List<String> doRealm) {
        return Papeis.EQUIPE.stream().filter(doRealm::contains).findFirst()
                .orElseThrow(() -> NegocioException.naoEncontrado("Usuário não encontrado entre os usuários da equipe."));
    }

    private static String exigirPapelDaEquipe(String papel) {
        String normalizado = Objects.requireNonNullElse(papel, "").trim().toLowerCase(Locale.ROOT);
        if (!Papeis.EQUIPE.contains(normalizado)) {
            throw NegocioException.invalido("papel-invalido",
                    "Papel inválido. Use gerencial, vendedor ou atendente. Contas de cliente nascem pelo primeiro acesso.");
        }
        return normalizado;
    }

    private boolean enviarConvite(String id) {
        try {
            keycloak.enviarEmailDefinirSenha(id);
            return true;
        } catch (RuntimeException e) {
            log.warn("Falha ao enviar o e-mail de definição de senha para o usuário {}", id, e);
            return false;
        }
    }

    private static boolean ehUsuarioTecnico(UsuarioKeycloak u) {
        return Usuario.SISTEMA_IMPORTACAO.toString().equals(u.id())
                || "sistema.importacao".equals(u.username())
                || (u.username() != null && u.username().startsWith("service-account-"));
    }

    private static UsuarioKeycloak desativado(UsuarioKeycloak u) {
        return new UsuarioKeycloak(u.id(), u.username(), u.firstName(), u.lastName(), u.email(), false,
                u.emailVerified(), u.requiredActions());
    }

    private static String nomeCompleto(UsuarioEquipe u) {
        return (Objects.requireNonNullElse(u.nome(), "") + " " + Objects.requireNonNullElse(u.sobrenome(), "")).trim();
    }

    private static UsuarioEquipe paraDto(UsuarioKeycloak u, String papel, long contratosVigentes) {
        boolean aguardandoSenha = u.requiredActions() != null && u.requiredActions().contains(ACAO_DEFINIR_SENHA);
        return new UsuarioEquipe(UUID.fromString(u.id()), u.username(), u.firstName(), u.lastName(), u.email(),
                papel, u.ativo(), aguardandoSenha, contratosVigentes);
    }
}
