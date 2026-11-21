package br.com.superauto.crm.relacionamento.infraestrutura.persistencia;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;

import br.com.superauto.crm.cadastro.repositorio.AtendimentoDoHistorico;
import br.com.superauto.crm.cadastro.repositorio.HistoricoDeAtendimentos;
import br.com.superauto.crm.cadastro.repositorio.LeitorDaFicha;
import br.com.superauto.crm.relacionamento.entidade.LinhaHistoricoAtendimento;
import br.com.superauto.crm.relacionamento.entidade.ResponsavelPelaCorrecao;
import br.com.superauto.crm.relacionamento.repositorio.HistoricoAtendimentoRepository;

import org.springframework.stereotype.Component;

/** O Relacionamento, dono dos atendimentos, atende a porta {@link HistoricoDeAtendimentos} do Cadastro. */
@Component
public class HistoricoDeAtendimentosJpa implements HistoricoDeAtendimentos {

    private final HistoricoAtendimentoRepository historico;
    private final Clock relogio;

    public HistoricoDeAtendimentosJpa(HistoricoAtendimentoRepository historico, Clock relogio) {
        this.historico = historico;
        this.relogio = relogio;
    }

    @Override
    public List<AtendimentoDoHistorico> doAssociado(Long associadoId, LeitorDaFicha leitor) {
        var responsavel = new ResponsavelPelaCorrecao(leitor.id(), leitor.gerencial());
        var agora = ZonedDateTime.now(relogio);
        return historico.findByAssociadoIdOrderByRealizadoEmDescIdDesc(associadoId).stream()
                .map(linha -> traduzir(linha, linha.podeSerCorrigidoPor(responsavel, agora)))
                .toList();
    }

    private static AtendimentoDoHistorico traduzir(LinhaHistoricoAtendimento linha, boolean podeCorrigir) {
        return new AtendimentoDoHistorico(linha.getId(), linha.getContratoId(), linha.getPlaca(), linha.getTipo(),
                linha.getEtapa(), linha.getResultado().name(), linha.getDescricao(), linha.getRetornoAgendadoPara(),
                linha.getRealizadoEm(), linha.getRegistradoPor(), linha.isCorrigido(), linha.getCorrigeAtendimentoId(),
                podeCorrigir);
    }
}
