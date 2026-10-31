package br.com.superauto.crm.acesso;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;

class ProjecaoUsuarioServiceTest {

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final ProjecaoUsuarioService servico = new ProjecaoUsuarioService(repositorio);

    @Test
    @DisplayName("No primeiro acesso grava id, username e nome; depois não vai mais ao banco")
    void gravaUmaVez() {
        UUID id = UUID.randomUUID();
        when(repositorio.registrar(any(), any(), any(), any())).thenReturn(1);

        servico.registrar(jwt(id.toString(), "vendedor1", "Vendedor Um"));
        servico.registrar(jwt(id.toString(), "vendedor1", "Vendedor Um"));

        verify(repositorio, times(1)).registrar(id, "vendedor1", "Vendedor Um", "vendedor1@x.com");
    }

    @Test
    @DisplayName("Nome alterado no Keycloak atualiza a projeção")
    void atualizaQuandoMuda() {
        UUID id = UUID.randomUUID();
        servico.registrar(jwt(id.toString(), "vendedor1", "Vendedor Um"));
        servico.registrar(jwt(id.toString(), "vendedor1", "Vendedor Um da Silva"));
        verify(repositorio).registrar(id, "vendedor1", "Vendedor Um da Silva", "vendedor1@x.com");
    }

    @Test
    @DisplayName("Sem nome no token usa o username (nome é obrigatório)")
    void semNome() {
        UUID id = UUID.randomUUID();
        servico.registrar(jwt(id.toString(), "atendente1", null));
        verify(repositorio).registrar(id, "atendente1", "atendente1", "atendente1@x.com");
    }

    @Test
    @DisplayName("Sub que não é UUID é ignorado")
    void subInvalido() {
        servico.registrar(jwt("nao-uuid", "x", "X"));
        verify(repositorio, never()).registrar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Falha no banco não derruba a requisição e tenta de novo na próxima")
    void falhaTentaDeNovo() {
        UUID id = UUID.randomUUID();
        when(repositorio.registrar(any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("uk_usuario_username"))
                .thenReturn(1);
        servico.registrar(jwt(id.toString(), "x", "X"));
        servico.registrar(jwt(id.toString(), "x", "X"));
        verify(repositorio, times(2)).registrar(id, "x", "X", "x@x.com");
    }

    private static Jwt jwt(String sub, String username, String nome) {
        var b = Jwt.withTokenValue("t").header("alg", "RS256").subject(sub)
                .claim("preferred_username", username).claim("email", username + "@x.com")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
        if (nome != null) {
            b.claim("name", nome);
        }
        return b.build();
    }
}
