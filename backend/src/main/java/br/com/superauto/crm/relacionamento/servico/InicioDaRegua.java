package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.cadastro.entidade.ContratoCadastrado;
import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoContrato;
import br.com.superauto.crm.relacionamento.repositorio.AcompanhamentoContratoRepository;
import br.com.superauto.crm.relacionamento.repositorio.ReguaDePosVenda;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * CAR-02 · Todo contrato cadastrado entra na régua de pós-venda. Roda antes do commit, na mesma transação do
 * cadastro: se a régua não puder ser aberta, pessoa, veículo e contrato também não são gravados.
 */
@Component
public class InicioDaRegua {

    private final AcompanhamentoContratoRepository acompanhamentos;
    private final ReguaDePosVenda regua;

    public InicioDaRegua(AcompanhamentoContratoRepository acompanhamentos, ReguaDePosVenda regua) {
        this.acompanhamentos = acompanhamentos;
        this.regua = regua;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoCadastrarContrato(ContratoCadastrado evento) {
        acompanhamentos.save(AcompanhamentoContrato.iniciar(evento.contratoId(), regua.etapas(), evento.dataAdesao()));
    }
}
