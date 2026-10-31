package br.com.superauto.crm.acesso.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.Conta;
import br.com.superauto.crm.acesso.repositorio.ProvedorIdentidade;
import br.com.superauto.crm.erro.NegocioException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransferenciaCarteiraTest {

    private final UUID origem = UUID.randomUUID();
    private final ProvedorEmMemoria provedor = new ProvedorEmMemoria();
    private final TransferenciaCarteira transferencia = new TransferenciaCarteira(provedor);

    @Test
    @DisplayName("Aceita como destino um vendedor ativo e diferente da origem")
    void destinoValido() {
        UUID destino = provedor.cadastrar("vendedor2", true, "vendedor");
        assertThat(transferencia.exigirVendedorDestino(origem, destino).username()).isEqualTo("vendedor2");
    }

    @Test
    @DisplayName("Recusa a própria origem, conta inexistente, conta desativada e quem não é vendedor")
    void destinosInvalidos() {
        UUID inativo = provedor.cadastrar("antigo", false, "vendedor");
        UUID atendente = provedor.cadastrar("atendente1", true, "atendente");

        assertThatThrownBy(() -> transferencia.exigirVendedorDestino(origem, origem)).hasMessageContaining("diferente");
        assertThatThrownBy(() -> transferencia.exigirVendedorDestino(origem, UUID.randomUUID())).hasMessageContaining("não existe");
        assertThatThrownBy(() -> transferencia.exigirVendedorDestino(origem, inativo)).hasMessageContaining("vendedor ativo");
        assertThatThrownBy(() -> transferencia.exigirVendedorDestino(origem, atendente)).hasMessageContaining("vendedor ativo");
    }

    @Test
    @DisplayName("Vendedor com contratos vigentes exige destino: sem ele, conflito com a quantidade")
    void destinoObrigatorio() {
        UUID destino = UUID.randomUUID();
        assertThat(TransferenciaCarteira.exigirDestinoInformado(destino, 3)).isEqualTo(destino);

        assertThatThrownBy(() -> TransferenciaCarteira.exigirDestinoInformado(null, 1))
                .isInstanceOfSatisfying(NegocioException.class, e -> {
                    assertThat(e.getTipo()).isEqualTo(NegocioException.Tipo.CONFLITO);
                    assertThat(e.getCodigo()).isEqualTo("contratos-a-transferir");
                    assertThat(e.getDados()).containsEntry("contratosVigentes", 1L);
                })
                .hasMessageContaining("1 contrato vigente.");
    }

    /** Dublê simples da porta: o domínio é testado sem Spring e sem framework de mock. */
    private static final class ProvedorEmMemoria implements ProvedorIdentidade {

        private final Map<UUID, Conta> contas = new HashMap<>();
        private final Map<UUID, List<String>> papeis = new HashMap<>();

        UUID cadastrar(String username, boolean ativo, String papel) {
            UUID id = UUID.randomUUID();
            contas.put(id, new Conta(id, username, null, null, null, ativo, false));
            papeis.put(id, List.of(papel));
            return id;
        }

        @Override
        public Optional<Conta> buscar(UUID id) {
            return Optional.ofNullable(contas.get(id));
        }

        @Override
        public List<String> papeis(UUID id) {
            return papeis.getOrDefault(id, List.of());
        }

        @Override
        public List<Conta> contasComPapel(String papel) {
            throw new UnsupportedOperationException();
        }

        @Override
        public UUID criar(NovaConta nova) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void definirAtivo(UUID id, boolean ativo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void encerrarSessoes(UUID id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void adicionarPapeis(UUID id, Collection<String> nomes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removerPapeis(UUID id, Collection<String> nomes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void enviarConviteDefinirSenha(UUID id) {
            throw new UnsupportedOperationException();
        }
    }
}
