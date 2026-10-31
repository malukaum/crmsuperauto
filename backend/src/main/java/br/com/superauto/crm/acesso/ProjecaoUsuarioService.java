package br.com.superauto.crm.acesso;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import br.com.superauto.crm.seguranca.UsuarioAtual;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

/**
 * Mantém a projeção local ({@code usuario}) de cada pessoa da equipe a partir do token do Keycloak.
 * Na primeira requisição depois do login a linha é criada; depois só é tocada se username, nome ou
 * e-mail mudarem no Keycloak. Um cache em memória evita ir ao banco a cada requisição.
 */
@Service
public class ProjecaoUsuarioService {

    private static final Logger log = LoggerFactory.getLogger(ProjecaoUsuarioService.class);

    private final UsuarioRepository repositorio;
    private final Map<UUID, Dados> conhecidos = new ConcurrentHashMap<>();

    public ProjecaoUsuarioService(UsuarioRepository repositorio) {
        this.repositorio = repositorio;
    }

    record Dados(String username, String nome, String email) {
    }

    /** Garante a projeção do dono do token. Tokens sem {@code sub} UUID (ex.: testes) são ignorados. */
    public void registrar(Jwt jwt) {
        UsuarioAtual.id(jwt).ifPresent(id -> registrar(id, dadosDo(jwt)));
    }

    /** Usado também pela gestão de usuários, que cria a projeção junto com a conta no Keycloak. */
    public void registrar(UUID id, String username, String nome, String email) {
        registrar(id, new Dados(username, nome, email));
    }

    private void registrar(UUID id, Dados dados) {
        if (dados.equals(conhecidos.get(id))) {
            return;
        }
        try {
            if (repositorio.registrar(id, dados.username(), dados.nome(), dados.email()) > 0) {
                log.info("Projeção local do usuário {} ({}) gravada", dados.username(), id);
            }
            conhecidos.put(id, dados);
        } catch (DataAccessException e) {
            // Ex.: username já usado por outra conta (recriada no Keycloak com outro id). Não derruba a requisição.
            log.error("Não foi possível gravar a projeção local do usuário {} ({})", dados.username(), id, e);
        }
    }

    private static Dados dadosDo(Jwt jwt) {
        String username = jwt.getClaimAsString("preferred_username");
        String nome = Objects.requireNonNullElse(jwt.getClaimAsString("name"), "").trim();
        return new Dados(username, nome.isEmpty() ? username : nome, jwt.getClaimAsString("email"));
    }
}
