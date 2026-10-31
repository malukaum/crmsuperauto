package br.com.superauto.crm.auditoria.servico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.entidade.OperacaoAuditoria;
import br.com.superauto.crm.cadastro.entidade.AssociadoAnonimizado;
import br.com.superauto.crm.cadastro.entidade.Classificacao;
import br.com.superauto.crm.cadastro.entidade.ClassificacaoAlterada;
import br.com.superauto.crm.cadastro.entidade.ContatoResumo;
import br.com.superauto.crm.cadastro.entidade.ContatosAlterados;
import br.com.superauto.crm.cadastro.entidade.DocumentoExibido;
import br.com.superauto.crm.cadastro.entidade.Perfil;
import br.com.superauto.crm.cadastro.entidade.PotencialIndicacao;
import br.com.superauto.crm.cadastro.entidade.TipoContatoAssociado;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditoriaDeAssociadosTest {

    private final RegistroAuditoria registro = mock(RegistroAuditoria.class);
    private final AuditoriaDeAssociados auditoria = new AuditoriaDeAssociados(registro);
    private final UUID autor = UUID.randomUUID();

    @Test
    @DisplayName("CAR-06: classificação registra só os campos que mudaram")
    void classificacao() {
        var promotor = new Classificacao(Perfil.PROMOTOR, PotencialIndicacao.NAO_AVALIADO);

        auditoria.aoClassificar(new ClassificacaoAlterada(7L, Classificacao.PADRAO, promotor, autor));

        var evento = registrado();
        assertThat(evento.getEntidade()).isEqualTo("associado");
        assertThat(evento.getDadosAnteriores()).isEqualTo(Map.of("perfil", "REGULAR"));
        assertThat(evento.getDadosNovos()).isEqualTo(Map.of("perfil", "PROMOTOR"));
    }

    @Test
    @DisplayName("CAR-04: contatos alterados registram a lista antes e depois")
    void contatos() {
        var antes = new ContatoResumo(TipoContatoAssociado.WHATSAPP, "5537998692950", true, true);
        var depois = new ContatoResumo(TipoContatoAssociado.WHATSAPP, "5537998692950", true, false);

        auditoria.aoAlterarContatos(new ContatosAlterados(7L, List.of(antes), List.of(depois), autor));

        assertThat(registrado().getDadosNovos()).extractingByKey("contatos").asInstanceOf(
                org.assertj.core.api.InstanceOfAssertFactories.LIST).singleElement()
                .isEqualTo(Map.of("tipo", "WHATSAPP", "valor", "5537998692950", "principal", true, "aceitaContato", false));
    }

    @Test
    @DisplayName("RG-04: ver o documento completo fica registrado como visualização")
    void visualizacao() {
        auditoria.aoExibirDocumento(new DocumentoExibido(7L, autor));

        assertThat(registrado().getOperacao()).isEqualTo(OperacaoAuditoria.VISUALIZACAO);
    }

    @Test
    @DisplayName("CAR-08: anonimização é registrada sem nenhum dado pessoal")
    void anonimizacao() {
        auditoria.aoAnonimizar(new AssociadoAnonimizado(7L, null, autor));

        var evento = registrado();
        assertThat(evento.getOperacao()).isEqualTo(OperacaoAuditoria.ANONIMIZACAO);
        assertThat(evento.getDadosAnteriores()).isNull();
        assertThat(evento.getDadosNovos()).isNull();
    }

    private EventoAuditoria registrado() {
        var captor = ArgumentCaptor.forClass(EventoAuditoria.class);
        verify(registro).registrar(captor.capture());
        return captor.getValue();
    }
}
