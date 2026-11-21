package br.com.superauto.crm.cadastro.servico;

import java.time.LocalDate;

import java.time.Clock;

import java.util.List;

import br.com.superauto.crm.cadastro.entidade.CarteiraExportada;
import br.com.superauto.crm.cadastro.entidade.LinhaCarteira;
import br.com.superauto.crm.cadastro.repositorio.ConsultaCarteira;
import br.com.superauto.crm.cadastro.repositorio.FiltroCarteira;
import br.com.superauto.crm.compartilhado.entidade.Telefone;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CAR-07 · Exportar a lista exatamente como filtrada na carteira. Cada exportação vai para a auditoria com o filtro
 * usado e a quantidade de linhas.
 */
@Service
public class ExportacaoCarteiraService {

    private final ConsultaCarteira carteira;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    public ExportacaoCarteiraService(ConsultaCarteira carteira, ApplicationEventPublisher eventos, Clock relogio) {
        this.carteira = carteira;
        this.eventos = eventos;
        this.relogio = relogio;
    }

    @Transactional
    public List<LinhaExportada> exportar(FiltroCarteira filtro, Ordenacao ordenacao, Exportador exportador) {
        FiltroCarteira noEscopo = ConsultaCarteiraService.noEscopo(filtro, exportador.escopo());
        LocalDate hoje = LocalDate.now(relogio);
        List<LinhaExportada> linhas = carteira.listar(noEscopo, ConsultaCarteiraService.ordem(ordenacao)).stream()
                .map(linha -> exportada(linha, exportador, hoje))
                .toList();
        eventos.publishEvent(new CarteiraExportada(noEscopo.informados(), linhas.size(), exportador.id()));
        return linhas;
    }

    private static LinhaExportada exportada(LinhaCarteira linha, Exportador exportador, LocalDate hoje) {
        String documento = linha.getDocumento()
                .map(d -> exportador.veDocumentoCompleto() ? d.formatado() : d.mascarado())
                .orElse("");
        return new LinhaExportada(linha.getAssociadoNome(), documento,
                linha.getWhatsapp().map(Telefone::formatado).orElse(""),
                linha.getWhatsapp().isPresent() && !linha.whatsappAceitaContato(),
                linha.getPlaca().valor(), linha.getModelo(), linha.getTipoVeiculo(), linha.getCidade(), linha.getUf(),
                linha.getMensalidade().valor(), linha.getSituacao(), linha.getEtapaPendente().orElse(""),
                linha.getProximoContatoEm().orElse(null), linha.filaEm(hoje), linha.diasDeAtrasoEm(hoje),
                linha.getVendedorNome().orElse(""));
    }
}
