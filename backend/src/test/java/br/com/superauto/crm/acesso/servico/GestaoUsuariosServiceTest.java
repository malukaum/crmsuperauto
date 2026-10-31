package br.com.superauto.crm.acesso.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.Conta;
import br.com.superauto.crm.acesso.entidade.PapelAlterado;
import br.com.superauto.crm.acesso.entidade.Usuario;
import br.com.superauto.crm.acesso.entidade.UsuarioCriado;
import br.com.superauto.crm.acesso.entidade.UsuarioDesativado;
import br.com.superauto.crm.acesso.repositorio.ContratosDoVendedor;
import br.com.superauto.crm.acesso.repositorio.ProvedorIdentidade;
import br.com.superauto.crm.acesso.repositorio.UsuarioRepository;
import br.com.superauto.crm.compartilhado.entidade.Email;
import br.com.superauto.crm.erro.IntegracaoIndisponivelException;
import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class GestaoUsuariosServiceTest {

    private final ProvedorIdentidade keycloak = mock(ProvedorIdentidade.class);
    private final ProjecaoUsuarioService projecao = mock(ProjecaoUsuarioService.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final ContratosDoVendedor contratos = mock(ContratosDoVendedor.class);
    private final ApplicationEventPublisher eventos = mock(ApplicationEventPublisher.class);

    private GestaoUsuariosService servico;

    private final UUID gerente = UUID.randomUUID();
    private final UUID vendedor = UUID.randomUUID();
    private final UUID outroVendedor = UUID.randomUUID();

    private final Usuario vendedorLocal = new Usuario(vendedor, "vendedor1", "Fulano vendedor1", null);

    @BeforeEach
    void setUp() {
        when(usuarios.findById(vendedor)).thenReturn(Optional.of(vendedorLocal));
        var transferencia = new TransferenciaCarteira(keycloak);
        servico = new GestaoUsuariosService(keycloak, projecao, usuarios, contratos, transferencia, eventos);
        when(keycloak.buscar(vendedor)).thenReturn(Optional.of(usuario(vendedor, "vendedor1", true)));
        when(keycloak.papeis(vendedor)).thenReturn(List.of("vendedor", "default-roles-crm"));
        when(keycloak.buscar(outroVendedor)).thenReturn(Optional.of(usuario(outroVendedor, "vendedor2", true)));
        when(keycloak.papeis(outroVendedor)).thenReturn(List.of("vendedor"));
    }

    @Test
    @DisplayName("Criar: conta no Keycloak com o papel, projeção local, evento para a auditoria e e-mail para definir a senha")
    void criar() {
        UUID novo = UUID.randomUUID();
        when(keycloak.criar(any())).thenReturn(novo);

        var criado = servico.criar(new NovoUsuario(" Maria.Souza ", "Maria", "Souza", "Maria@SuperAuto.local", "Atendente"), gerente);

        assertThat(criado.emailEnviado()).isTrue();
        assertThat(criado.usuario().papel()).isEqualTo("atendente");
        assertThat(criado.usuario().aguardandoSenha()).isTrue();
        verify(keycloak).adicionarPapeis(novo, List.of("atendente"));
        verify(projecao).registrar(new Conta(novo, "maria.souza", "Maria", "Souza", Email.de("maria@superauto.local"), true, true));
        verify(eventos).publishEvent(new UsuarioCriado(novo, "maria.souza", "Maria Souza",
                Email.de("maria@superauto.local"), "atendente", gerente));
        verify(keycloak).enviarConviteDefinirSenha(novo);
    }

    @Test
    @DisplayName("Criar: falha no e-mail não desfaz a criação, só avisa")
    void criarComFalhaNoEmail() {
        when(keycloak.criar(any())).thenReturn(UUID.randomUUID());
        doThrow(new IntegracaoIndisponivelException("smtp", null)).when(keycloak).enviarConviteDefinirSenha(any());

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
                    assertThat(e.getTipo()).isEqualTo(NegocioException.Tipo.CONFLITO);
                    assertThat(e.getCodigo()).isEqualTo("contratos-a-transferir");
                    assertThat(e.getDados()).containsEntry("contratosVigentes", 7L);
                });
        verify(keycloak, never()).definirAtivo(any(), eq(false));
        verify(eventos, never()).publishEvent(any(Object.class));
        assertThat(vendedorLocal.isAtivo()).as("projeção local intacta").isTrue();
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
        ordem.verify(usuarios).findById(vendedor);
        ordem.verify(keycloak).definirAtivo(vendedor, false);
        ordem.verify(keycloak).encerrarSessoes(vendedor);
        verify(eventos).publishEvent(new UsuarioDesativado(vendedor, true, 7, outroVendedor, gerente));
        assertThat(vendedorLocal.isAtivo()).as("projeção local desativada").isFalse();
        verify(projecao).registrar(usuario(outroVendedor, "vendedor2", true));
    }

    @Test
    @DisplayName("Transferência só para vendedor ativo e diferente do desativado")
    void transferenciaInvalida() {
        when(contratos.contar(vendedor)).thenReturn(2L);
        when(keycloak.buscar(outroVendedor)).thenReturn(Optional.of(usuario(outroVendedor, "vendedor2", false)));

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
        verify(keycloak).definirAtivo(vendedor, false);
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
        verify(keycloak).adicionarPapeis(vendedor, List.of("atendente"));
        verify(keycloak).removerPapeis(vendedor, List.of("vendedor"));
        verify(keycloak).encerrarSessoes(vendedor);
        verify(eventos).publishEvent(new PapelAlterado(vendedor, "vendedor", "atendente", gerente));
    }

    @Test
    @DisplayName("Contas sem papel da equipe (ex.: cliente) não são geridas por aqui")
    void naoGereCliente() {
        UUID cliente = UUID.randomUUID();
        when(keycloak.buscar(cliente)).thenReturn(Optional.of(usuario(cliente, "cliente1", true)));
        when(keycloak.papeis(cliente)).thenReturn(List.of("cliente"));

        assertThatThrownBy(() -> servico.desativar(cliente, null, gerente))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(NegocioException.Tipo.NAO_ENCONTRADO));
    }

    @Test
    @DisplayName("Conta inexistente no provedor responde não encontrado")
    void contaInexistente() {
        UUID sumido = UUID.randomUUID();
        when(keycloak.buscar(sumido)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.reativar(sumido, gerente))
                .isInstanceOfSatisfying(NegocioException.class,
                        e -> assertThat(e.getTipo()).isEqualTo(NegocioException.Tipo.NAO_ENCONTRADO));
    }

    private static Conta usuario(UUID id, String username, boolean ativo) {
        return new Conta(id, username, "Fulano", username, null, ativo, false);
    }
}
