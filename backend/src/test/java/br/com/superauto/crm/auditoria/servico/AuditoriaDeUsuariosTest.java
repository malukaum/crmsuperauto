package br.com.superauto.crm.auditoria.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.PapelAlterado;
import br.com.superauto.crm.acesso.entidade.UsuarioCriado;
import br.com.superauto.crm.acesso.entidade.UsuarioDesativado;
import br.com.superauto.crm.acesso.entidade.UsuarioReativado;
import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.entidade.OperacaoAuditoria;
import br.com.superauto.crm.compartilhado.entidade.Email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditoriaDeUsuariosTest {

    private final RegistroAuditoria registro = mock(RegistroAuditoria.class);
    private final AuditoriaDeUsuarios auditoria = new AuditoriaDeUsuarios(registro);

    private final UUID usuario = UUID.randomUUID();
    private final UUID gerente = UUID.randomUUID();

    @Test
    @DisplayName("Criação registra os dados da conta, sem valores anteriores")
    void criacao() {
        auditoria.aoCriar(new UsuarioCriado(usuario, "maria", "Maria Souza", Email.de("maria@x.com"), "atendente", gerente));

        var evento = registrado();
        assertThat(evento.getOperacao()).isEqualTo(OperacaoAuditoria.CRIACAO);
        assertThat(evento.getEntidade()).isEqualTo("usuario");
        assertThat(evento.getEntidadeId()).isEqualTo(usuario.toString());
        assertThat(evento.getDadosAnteriores()).isNull();
        assertThat(evento.getDadosNovos()).containsEntry("email", "maria@x.com").containsEntry("papel", "atendente")
                .containsEntry("ativo", true);
    }

    @Test
    @DisplayName("Troca de papel registra o papel anterior e o novo")
    void papel() {
        auditoria.aoAlterarPapel(new PapelAlterado(usuario, "vendedor", "atendente", gerente));

        var evento = registrado();
        assertThat(evento.getDadosAnteriores()).isEqualTo(Map.of("papel", "vendedor"));
        assertThat(evento.getDadosNovos()).isEqualTo(Map.of("papel", "atendente"));
    }

    @Test
    @DisplayName("Desativação registra a transferência de contratos quando houve")
    void desativacao() {
        UUID destino = UUID.randomUUID();
        auditoria.aoDesativar(new UsuarioDesativado(usuario, true, 3, destino, gerente));

        var evento = registrado();
        assertThat(evento.getDadosAnteriores()).isEqualTo(Map.of("ativo", true));
        assertThat(evento.getDadosNovos()).containsEntry("ativo", false).containsEntry("contratosTransferidos", 3)
                .containsEntry("transferidosPara", destino.toString());
    }

    @Test
    @DisplayName("Reativação registra o ativo anterior e o novo")
    void reativacao() {
        auditoria.aoReativar(new UsuarioReativado(usuario, false, gerente));

        var evento = registrado();
        assertThat(evento.getDadosAnteriores()).isEqualTo(Map.of("ativo", false));
        assertThat(evento.getDadosNovos()).isEqualTo(Map.of("ativo", true));
    }

    private EventoAuditoria registrado() {
        var captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(registro).registrar(captor.capture());
        return captor.getValue();
    }
}
