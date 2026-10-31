package br.com.superauto.crm.cadastro.repositorio;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A régua de pós-venda de cada contrato, para a ficha (CAR-03). Porta declarada pelo Cadastro e implementada
 * pelo Relacionamento, dono do acompanhamento dos contratos.
 */
public interface ReguaDosContratos {

    /** Contrato sem acompanhamento não aparece no mapa. */
    Map<Long, List<EtapaPrevista>> previsoes(Collection<Long> contratoIds);
}
