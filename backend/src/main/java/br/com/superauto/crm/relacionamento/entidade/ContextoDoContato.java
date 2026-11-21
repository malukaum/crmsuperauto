package br.com.superauto.crm.relacionamento.entidade;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * O que o registro de um contato precisa saber além do resultado (ATD-01).
 *
 * @param versao a versão do acompanhamento lida na fila ou na ficha (RG-06)
 * @param realizadoEm quando o atendimento aconteceu, no fuso de São Paulo
 */
public record ContextoDoContato(int versao, LocalDate hoje, OffsetDateTime realizadoEm, List<EtapaRegua> etapas) {

    public ContextoDoContato {
        etapas = List.copyOf(etapas);
    }

    LocalDate dataDoAtendimento() {
        return realizadoEm.toLocalDate();
    }

    EtapaRegua etapa(Short id) {
        return EtapaRegua.daRegua(etapas, id);
    }
}
