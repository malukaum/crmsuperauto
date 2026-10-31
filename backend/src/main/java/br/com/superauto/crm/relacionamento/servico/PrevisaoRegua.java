package br.com.superauto.crm.relacionamento.servico;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import br.com.superauto.crm.relacionamento.entidade.EtapaNaRegua;
import br.com.superauto.crm.relacionamento.entidade.EtapaNaRegua.Andamento;
import br.com.superauto.crm.relacionamento.entidade.EtapaRegua;

/**
 * Serviço de domínio (regra pura): mostra a régua de um contrato para frente (CAR-03). As etapas antes da pendente
 * estão concluídas; a pendente vale na data do próximo contato; cada futura vem depois da anterior pelo prazo dela
 * (RG-13). Régua encerrada (sem etapa pendente) fica toda concluída.
 */
public final class PrevisaoRegua {

    private PrevisaoRegua() {
    }

    /** @param etapas a régua, na ordem em que acontece */
    public static List<EtapaNaRegua> prever(List<EtapaRegua> etapas, Optional<Short> pendenteId,
                                            Optional<LocalDate> proximoContato) {
        List<EtapaNaRegua> previsao = new ArrayList<>();
        Andamento andamento = Andamento.CONCLUIDA;
        Optional<LocalDate> data = Optional.empty();
        for (EtapaRegua etapa : etapas) {
            if (andamento == Andamento.CONCLUIDA && pendenteId.filter(etapa.id()::equals).isPresent()) {
                andamento = Andamento.PENDENTE;
                data = proximoContato;
            } else if (andamento != Andamento.CONCLUIDA) {
                andamento = Andamento.FUTURA;
                data = data.map(anterior -> anterior.plusDays(etapa.diasAposAnterior()));
            }
            previsao.add(new EtapaNaRegua(etapa, andamento, andamento == Andamento.CONCLUIDA ? Optional.empty() : data));
        }
        return List.copyOf(previsao);
    }
}
