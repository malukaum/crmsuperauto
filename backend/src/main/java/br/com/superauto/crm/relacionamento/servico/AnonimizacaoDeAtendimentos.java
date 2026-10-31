package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.cadastro.entidade.AssociadoAnonimizado;
import br.com.superauto.crm.relacionamento.entidade.Atendimento;
import br.com.superauto.crm.relacionamento.repositorio.AtendimentoRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * CAR-08 · Ao anonimizar o associado, a descrição dos atendimentos dele é apagada, porque pode conter dados pessoais
 * (proposta). Na mesma transação da anonimização; a escrita sai por dirty checking no commit.
 */
@Component
public class AnonimizacaoDeAtendimentos {

    private final AtendimentoRepository atendimentos;

    public AnonimizacaoDeAtendimentos(AtendimentoRepository atendimentos) {
        this.atendimentos = atendimentos;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoAnonimizar(AssociadoAnonimizado evento) {
        atendimentos.findByAssociadoId(evento.associadoId()).forEach(Atendimento::apagarDescricaoPorAnonimizacao);
    }
}
