package br.com.superauto.crm.cadastro.api;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.servico.LinhaExportada;
import br.com.superauto.crm.compartilhado.entidade.Fila;

/**
 * As colunas do arquivo exportado da carteira (CAR-07), iguais em todos os formatos. Os valores saem tipados
 * (texto, valor em reais, data, número) para cada formato aplicar a RG-02 do seu jeito.
 */
final class ColunasExportacao {

    /** Valor em reais (mensalidade contratada). */
    record Reais(BigDecimal valor) {
    }

    private static final String SIM = "Sim";
    private static final String NAO = "Não";

    /** RG-09: a coluna diz que o valor é contratado, não recebido. */
    static final List<String> CABECALHO = List.of("Cliente", "CPF/CNPJ", "WhatsApp", "Aceita contato", "Placa",
            "Modelo", "Tipo de veículo", "Cidade", "UF", "Mensalidade contratada (não é receita)", "Situação",
            "Etapa pendente", "Próximo contato", "Fila", "Dias de atraso", "Vendedor");

    private static final Map<SituacaoContrato, String> SITUACOES = Map.of(
            SituacaoContrato.A_CONFERIR, "A conferir",
            SituacaoContrato.ATIVO, "Ativo",
            SituacaoContrato.SUSPENSO, "Suspenso",
            SituacaoContrato.CANCELADO, "Cancelado");

    private static final Map<Fila, String> FILAS = Map.of(
            Fila.HOJE, "Hoje",
            Fila.ATRASADO, "Atrasado",
            Fila.FUTURO, "Futuro",
            Fila.SEM_DATA, "Sem data",
            Fila.SUSPENSO, "Suspenso",
            Fila.CANCELADO, "Cancelado");

    private ColunasExportacao() {
    }

    /**
     * Os valores de uma linha, na ordem do {@link #CABECALHO}: {@code String}, {@link Reais}, {@code LocalDate},
     * {@code Integer} ou {@code null} (célula vazia).
     */
    static List<Object> valores(LinhaExportada l) {
        return Arrays.asList(l.associado(), l.documento(), l.whatsapp(), aceitaContato(l), l.placa(), l.modelo(),
                l.tipoVeiculo(), l.cidade(), l.uf(), new Reais(l.mensalidade()), SITUACOES.get(l.situacao()),
                l.etapaPendente(), l.proximoContato(), FILAS.get(l.fila()), l.diasAtraso(), l.vendedor());
    }

    /** Contatos com opt-out saem marcados; sem WhatsApp, a coluna fica vazia. */
    private static String aceitaContato(LinhaExportada l) {
        if (l.whatsapp().isEmpty()) {
            return "";
        }
        return l.whatsappSemContato() ? NAO : SIM;
    }
}
