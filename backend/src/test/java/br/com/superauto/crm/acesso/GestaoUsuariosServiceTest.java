package br.com.superauto.crm.acesso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.superauto.crm.acesso.GestaoUsuariosService.NovoUsuario;
import br.com.superauto.crm.acesso.KeycloakAdminClient.UsuarioKeycloak;
import br.com.superauto.crm.auditoria.RegistroAuditoria;
import br.com.superauto.crm.erro.IntegracaoIndisponivelException;
import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class GestaoUsuariosServiceTest {

    private final KeycloakAdminClient keycloak = mock(KeycloakAdminClient.class);
    private final ProjecaoUsuarioService projecao = mock(ProjecaoUsuarioService.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ContratosDoVendedor contratos = mock(ContratosDoVendedor.class);
    private final RegistroAuditoria auditoria = mock(RegistroAuditoria.class);

    private GestaoUsuariosService servico;

    private final UUID gerente = UUID.randomUUID();
    private final UUID vendedor = UUID.randomUUID();
    private final UUID outroVendedor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        servico = new GestaoUsuariosService(keycloak, projecao, usuarios, contratos, auditoria);
        when(keycloak.buscar(vendedor.toString())).thenReturn(usuario(vendedor, "vendedor1", true));
        when(keycloak.papeisDoRealm(vendedor.toString())).thenReturn(List.of("vendedor", "default-roles-crm"));
        when(keycloak.buscar(outroVendedor.toString())).thenReturn(usuario(outroVendedor, "vendedor2", true));
        when(keycloak.papeisDoRealm(outroVendedor.toString())).thenReturn(List.of("vendedor"));
    }

    @Test
    @DisplayName("Lista só a equipe, com o papel mais amplo, sem contas técnicas e com os contratos de cada um")
    void listar() {
        var gerenteKc = usuario(gerente, "gerente", true);
        var dupla = usuario(UUID.randomUUID(), "duplo", true);
        var inativo = usuario(UUID.randomUUID(), "antigo", false);
        var servicoKc = usuario(UUID.randomUUID(), "service-account-crm-api", true);
        when(keycloak.usuariosComPapel("gerencial")).thenReturn(List.of(gerenteKc, dupla));
        when(keycloak.usuariosComPapel("vendedor")).thenReturn(List.of(usuario(vendedor, "vendedor1", true), dupla, inativo));
        when(keycloak.usuariosComPapel("atendente")).thenReturn(List.of(servicoKc));
        when(contratos.contarPorVendedor()).thenReturn(Map.of(vendedor, 12L));

        var lista = servico.listar();

        assertThat(lista).extracting(GestaoUsuariosService.UsuarioEquipe::username)
                .containsExactlyInAnyOrder("gerente", "duplo", "vendedor1", "antigo");
        assertThat(lista.getLast().username()).as("desativados no fim").isEqualTo("antigo");
        assertThat(lista).filteredOn(u -> u.username().equals("duplo")).first()
                .satisfies(u -> assertThat(u.papel()).isEqualTo("gerencial"));
        assertThat(lista).filteredOn(u -> u.username().equals("vendedor1")).first()
                .satisfies(u -> assertThat(u.contratosVigentes()).isEqualTo(12));
    }

    @Test
    @DisplayName("Criar: conta no Keycloak com o papel, projeção local, auditoria e e-mail para definir a senha")
    void criar() {
        UUID novo = UUID.randomUUID();
        when(keycloak.criar(any())).thenReturn(novo.toString());

        var criado = servico.criar(new NovoUsuario(" Maria.Souza ", "Maria", "Souza", "Maria@SuperAuto.local", "Atendente"), gerente);

        assertThat(criado.emailEnviado()).isTrue();
        assertThat(criado.usuario().papel()).isEqualTo("atendente");
        assertThat(criado.usuario().aguardandoSenha()).isTrue();
        verify(keycloak).adicionarPapeis(novo.toString(), List.of("atendente"));
        verify(projecao).registrar(novo, "maria.souza", "Maria Souza", "maria@superauto.local");
        verify(auditoria).registrar(eq("usuario"), eq(novo.toString()), eq("CRIACAO"), eq(gerente), eq(null), any());
        verify(keycloak).enviarEmailDefinirSenha(novo.toString());
    }

    @Test
    @DisplayName("Criar: falha no e-mail não desfaz a criação, só avisa")
    void criarComFalhaNoEmail() {
        when(keycloak.criar(any())).thenReturn(UUID.randomUUID().toString());
        doThrow(new IntegracaoIndisponivelException("smtp", null)).when(keycloak).enviarEmailDefinirSenha(anyString());

        assertThat(servico.criar(new NovoUsuario("joao", "João", "Lima", "joao@x.com", "vendedor"), gerente).emailEnviado())
                .isFalse();
    }

    @Test
    @DisplayName("Contas de cliente nunca nascem pela gestão de usuários")
    void naoCriaCliente() {
        assertThatThrownBy(() -> servico.criar(new NovoUsuario("c", "C", "D", "c@x.com", "cliente"), gerente))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("primeiro acesso");
        verify(keycloak, never()).criar(any());
    }

    @Test
    @DisplayName("Desativar vendedor com contratos vigentes sem destino: 409 com a quantidade, nada é alterado")
    void desativarVendedorComContratosExigeTransferencia() {
        when(contratos.contar(vendedor)).thenReturn(7L);

        assertThatThrownBy(() -> servico.desativar(vendedor, null, gerente))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getCodigo()).isEqualTo("contratos-a-transferir");
                    assertThat(e.getDados()).containsEntry("contratosVigentes", 7L);
                });
        verify(keycloak, never()).definirAtivo(anyString(), eq(false));
        verify(usuarios, never()).definirAtivo(any(), eq(false));
    }

    @Test
    @DisplayName("Desativar com transferência: contratos passam ao outro vendedor e só então o login é desligado")
    void desativarComTransferencia() {
        when(contratos.contar(vendedor)).thenReturn(7L);
        when(contratos.transferir(vendedor, outroVendedor, gerente)).thenReturn(7);

        var resultado = servico.desativar(vendedor, outroVendedor, gerente);

        assertThat(resultado.ativo()).isFalse();
        var ordem = inOrder(contratos, usuarios, keycloak);
        ordem.verify(contratos).transferir(vendedor, outroVendedor, gerente);
        ordem.verify(usuarios).definirAtivo(vendedor, false);
        ordem.verify(keycloak).definirAtivo(vendedor.toString(), false);
        ordem.verify(keycloak).encerrarSessoes(vendedor.toString());
        verify(projecao).registrar(outroVendedor, "vendedor2", "Fulano vendedor2", null);
    }

    @Test
    @DisplayName("Transferência só para vendedor ativo e diferente do desativado")
    void transferenciaInvalida() {
        when(contratos.contar(vendedor)).thenReturn(2L);
        when(keycloak.buscar(outroVendedor.toString())).thenReturn(usuario(outroVendedor, "vendedor2", false));

        assertThatThrownBy(() -> servico.desativar(vendedor, outroVendedor, gerente))
                .hasMessageContaining("vendedor ativo");
        assertThatThrownBy(() -> servico.desativar(vendedor, vendedor, gerente))
                .hasMessageContaining("diferente");
        verify(contratos, never()).transferir(any(), any(), any());
    }

    @Test
    @DisplayName("Vendedor sem contratos é desativado direto")
    void desativarSemContratos() {
        when(contratos.contar(vendedor)).thenReturn(0L);
        servico.desativar(vendedor, null, gerente);
        verify(contratos, never()).transferir(any(), any(), any());
        verify(keycloak).definirAtivo(vendedor.toString(), false);
    }

    @Test
    @DisplayName("Ninguém desativa nem troca o papel de si mesmo")
    void protecaoDoProprioUsuario() {
        assertThatThrownBy(() -> servico.desativar(gerente, null, gerente)).hasMessageContaining("próprio");
        assertThatThrownBy(() -> servico.alterarPapel(gerente, "atendente", gerente)).hasMessageContaining("próprio");
    }

    @Test
    @DisplayName("Trocar papel: adiciona o novo, remove os outros da equipe e derruba as sessões")
    void alterarPapel() {
        var r = servico.alterarPapel(vendedor, "atendente", gerente);

        assertThat(r.papel()).isEqualTo("atendente");
        verify(keycloak).adicionarPapeis(vendedor.toString(), List.of("atendente"));
        verify(keycloak).removerPapeis(vendedor.toString(), List.of("vendedor"));
        verify(keycloak).encerrarSessoes(vendedor.toString());
        verify(auditoria).registrar("usuario", vendedor.toString(), "ALTERACAO", gerente,
                Map.of("papel", "vendedor"), Map.of("papel", "atendente"));
    }

    @Test
    @DisplayName("Contas sem papel da equipe (ex.: cliente) não são geridas por aqui")
    void naoGereCliente() {
        UUID cliente = UUID.randomUUID();
        when(keycloak.buscar(cliente.toString())).thenReturn(usuario(cliente, "cliente1", true));
        when(keycloak.papeisDoRealm(cliente.toString())).thenReturn(List.of("cliente"));

        assertThatThrownBy(() -> servico.desativar(cliente, null, gerente))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static UsuarioKeycloak usuario(UUID id, String username, boolean ativo) {
        return new UsuarioKeycloak(id.toString(), username, "Fulano", username, null, ativo, true, List.of());
    }
}
