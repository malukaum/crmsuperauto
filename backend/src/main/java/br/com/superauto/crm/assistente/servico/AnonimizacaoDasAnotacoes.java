package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.repositorio.AnotacaoDoAtendimentoRepository;
import br.com.superauto.crm.cadastro.entidade.AssociadoAnonimizado;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * IA-06 / CAR-08 · Ao anonimizar o associado, apaga as anotações, os anexos e os textos dos resumos dele, na mesma
 * transação. Os sinais não identificáveis (assunto, satisfação, risco) continuam nos indicadores.
 */
@Component
@RequiredArgsConstructor
public class AnonimizacaoDasAnotacoes {
    private final AnotacaoDoAtendimentoRepository anotacoes;
    private final Clock relogio;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoAnonimizar(AssociadoAnonimizado evento) {
        var agora = OffsetDateTime.now(relogio);
        anotacoes.buscarDoAssociadoParaAlteracao(evento.associadoId()).forEach(a -> a.anonimizar(agora));
    }
}
