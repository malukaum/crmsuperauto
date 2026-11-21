package br.com.superauto.crm.relacionamento.api;

import br.com.superauto.crm.exportacao.Planilha;
import br.com.superauto.crm.relacionamento.servico.LinhaDoHistorico;

import java.util.Arrays;
import java.util.List;

/** ATD-05 · As colunas do histórico exportado, como na aba ATENDIMENTOS da planilha. */
final class ColunasDoHistorico {
    private static final String ABA = "Atendimentos";
    private static final String CORRIGIDO = "Corrigido";
    private static final String CORRECAO = "Correção";

    static final List<String> CABECALHO = List.of("Data e hora", "Placa", "Cliente", "Tipo", "Etapa", "Resultado",
            "Descrição", "Próxima ação", "Data de retorno", "Usuário", "Cidade", "Correção");

    private ColunasDoHistorico() {
    }

    static Planilha planilha(List<LinhaDoHistorico> linhas) {
        return new Planilha(ABA, CABECALHO, linhas.stream().map(ColunasDoHistorico::valores).toList());
    }

    private static List<Object> valores(LinhaDoHistorico l) {
        return Arrays.asList(l.realizadoEm().toLocalDateTime(), l.placa(), l.associado(), l.tipo(), l.etapa(),
                l.resultado().descricao(), l.descricao(), l.proximaAcao(), l.retornoAgendadoPara(), l.registradoPor(),
                l.cidade(), marcaDeCorrecao(l));
    }

    private static String marcaDeCorrecao(LinhaDoHistorico linha) {
        if (linha.corrigido()) return CORRIGIDO;
        return linha.corrigeAtendimentoId() == null ? null : CORRECAO;
    }
}
