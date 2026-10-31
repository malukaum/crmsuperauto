package br.com.superauto.crm.acesso.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.Conta;
import br.com.superauto.crm.acesso.entidade.Usuario;
import br.com.superauto.crm.acesso.repositorio.UsuarioRepository;
import br.com.superauto.crm.compartilhado.entidade.Email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionOperations;

class ProjecaoUsuarioServiceTest {

    private final UsuarioRepository repositorio = mock(UsuarioRepository.class);
    private final ProjecaoUsuarioService servico =
            new ProjecaoUsuarioService(repositorio, TransactionOperations.withoutTransaction());

    /** "Banco" em memória: o que foi salvo passa a ser encontrado. */
    private final Map<UUID, Usuario> banco = new HashMap<>();

    @BeforeEach
    void setUp() {
        when(repositorio.findById(any())).thenAnswer(i -> Optional.ofNullable(banco.get(i.<UUID>getArgument(0))));
        when(repositorio.save(any())).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            banco.put(u.getId(), u);
            return u;
        });
    }

    @Test
    @DisplayName("No primeiro acesso cria a projeção ativa; depois não vai mais ao banco")
    void gravaUmaVez() {
        UUID id = UUID.randomUUID();

        servico.registrar(id, new DadosProjecao("vendedor1", "Vendedor Um", Email.de("vendedor1@x.com")));
        servico.registrar(id, new DadosProjecao("vendedor1", "Vendedor Um", Email.de("vendedor1@x.com")));

        var salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(repositorio, times(1)).save(salvo.capture());
        assertThat(salvo.getValue().isNew()).isTrue();
        assertThat(salvo.getValue().isAtivo()).isTrue();
        assertThat(salvo.getValue().getNome()).isEqualTo("Vendedor Um");
        verify(repositorio, times(1)).findById(id);
    }

    @Test
    @DisplayName("Nome alterado no Keycloak atualiza a entidade (sem novo INSERT)")
    void atualizaQuandoMuda() {
        UUID id = UUID.randomUUID();
        servico.registrar(id, new DadosProjecao("vendedor1", "Vendedor Um", Email.de("vendedor1@x.com")));
        servico.registrar(id, new DadosProjecao("vendedor1", "Vendedor Um da Silva", Email.de("vendedor1@x.com")));

        verify(repositorio, times(1)).save(any());
        assertThat(banco.get(id).getNome()).isEqualTo("Vendedor Um da Silva");
    }

    @Test
    @DisplayName("Atualizar dados não mexe em ativo")
    void naoReativa() {
        UUID id = UUID.randomUUID();
        var existente = new Usuario(id, "antigo", "Antigo", null);
        existente.definirAtivo(false);
        banco.put(id, existente);

        servico.registrar(id, new DadosProjecao("antigo", "Antigo Nome Novo", null));

        assertThat(existente.isAtivo()).isFalse();
        verify(repositorio, never()).save(any());
    }

    @Test
    @DisplayName("Sem nome usa o username (nome é obrigatório)")
    void semNome() {
        UUID id = UUID.randomUUID();
        servico.registrar(id, new DadosProjecao("atendente1", null, Email.de("atendente1@x.com")));
        servico.registrar(id, new DadosProjecao("atendente1", "  ", Email.de("atendente1@x.com")));
        assertThat(banco.get(id).getNome()).isEqualTo("atendente1");
        verify(repositorio, times(1)).findById(id);
    }

    @Test
    @DisplayName("A partir de uma conta usa nome e sobrenome")
    void aPartirDeConta() {
        UUID id = UUID.randomUUID();
        servico.registrar(new Conta(id, "maria", "Maria", "Souza", Email.de("maria@x.com"), true, false));
        assertThat(banco.get(id).getNome()).isEqualTo("Maria Souza");
    }

    @Test
    @DisplayName("Falha no banco não derruba a requisição e tenta de novo na próxima")
    void falhaTentaDeNovo() {
        UUID id = UUID.randomUUID();
        doThrow(new DataIntegrityViolationException("uk_usuario_username"))
                .doAnswer(i -> i.getArgument(0))
                .when(repositorio).save(any());

        servico.registrar(id, new DadosProjecao("x", "X", Email.de("x@x.com")));
        servico.registrar(id, new DadosProjecao("x", "X", Email.de("x@x.com")));

        verify(repositorio, times(2)).save(any());
    }
}
