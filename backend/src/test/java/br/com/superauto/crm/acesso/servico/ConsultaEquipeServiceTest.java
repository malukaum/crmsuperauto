package br.com.superauto.crm.acesso.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.Conta;
import br.com.superauto.crm.acesso.repositorio.ContratosDoVendedor;
import br.com.superauto.crm.acesso.repositorio.ProvedorIdentidade;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConsultaEquipeServiceTest {

    private final ProvedorIdentidade keycloak = mock(ProvedorIdentidade.class);
    private final ContratosDoVendedor contratos = mock(ContratosDoVendedor.class);
    private final ConsultaEquipeService consulta = new ConsultaEquipeService(keycloak, contratos);

    private final UUID gerente = UUID.randomUUID();
    private final UUID vendedor = UUID.randomUUID();

    @Test
    @DisplayName("Lista só a equipe, com o papel mais amplo, sem contas técnicas e com os contratos de cada um")
    void listar() {
        var gerenteKc = usuario(gerente, "gerente", true);
        var dupla = usuario(UUID.randomUUID(), "duplo", true);
        var inativo = usuario(UUID.randomUUID(), "antigo", false);
        var servicoKc = usuario(UUID.randomUUID(), "service-account-crm-api", true);
        when(keycloak.contasComPapel("gerencial")).thenReturn(List.of(gerenteKc, dupla));
        when(keycloak.contasComPapel("vendedor")).thenReturn(List.of(usuario(vendedor, "vendedor1", true), dupla, inativo));
        when(keycloak.contasComPapel("atendente")).thenReturn(List.of(servicoKc));
        when(contratos.contarPorVendedor()).thenReturn(Map.of(vendedor, 12L));

        var lista = consulta.listar();

        assertThat(lista).extracting(UsuarioEquipe::username)
                .containsExactlyInAnyOrder("gerente", "duplo", "vendedor1", "antigo");
        assertThat(lista.getLast().username()).as("desativados no fim").isEqualTo("antigo");
        assertThat(lista).filteredOn(u -> u.username().equals("duplo")).first()
                .satisfies(u -> assertThat(u.papel()).isEqualTo("gerencial"));
        assertThat(lista).filteredOn(u -> u.username().equals("vendedor1")).first()
                .satisfies(u -> assertThat(u.contratosVigentes()).isEqualTo(12));
    }

    private static Conta usuario(UUID id, String username, boolean ativo) {
        return new Conta(id, username, "Fulano", username, null, ativo, false);
    }
}
