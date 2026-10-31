package br.com.superauto.crm.relacionamento.infraestrutura.persistencia;

import java.util.List;

import br.com.superauto.crm.cadastro.repositorio.AtendimentoDoHistorico;
import br.com.superauto.crm.cadastro.repositorio.HistoricoDeAtendimentos;
import br.com.superauto.crm.relacionamento.entidade.LinhaHistoricoAtendimento;
import br.com.superauto.crm.relacionamento.repositorio.HistoricoAtendimentoRepository;

import org.springframework.stereotype.Component;

/** O Relacionamento, dono dos atendimentos, atende a porta {@link HistoricoDeAtendimentos} do Cadastro. */
@Component
public class HistoricoDeAtendimentosJpa implements HistoricoDeAtendimentos {

    private final HistoricoAtendimentoRepository historico;

    public HistoricoDeAtendimentosJpa(HistoricoAtendimentoRepository historico) {
        this.historico = historico;
    }

    @Override
    public List<AtendimentoDoHistorico> doAssociado(Long associadoId) {
        return historico.findByAssociadoIdOrderByRealizadoEmDescIdDesc(associadoId).stream()
                .map(HistoricoDeAtendimentosJpa::traduzir)
                .toList();
    }

    private static AtendimentoDoHistorico traduzir(LinhaHistoricoAtendimento linha) {
        return new AtendimentoDoHistorico(linha.getId(), linha.getContratoId(), linha.getPlaca(), linha.getTipo(),
                linha.getEtapa(), linha.getResultado(), linha.getDescricao(), linha.getRetornoAgendadoPara(),
                linha.getRealizadoEm(), linha.getRegistradoPor(), linha.isCorrigido(), linha.getCorrigeAtendimentoId());
    }
}
