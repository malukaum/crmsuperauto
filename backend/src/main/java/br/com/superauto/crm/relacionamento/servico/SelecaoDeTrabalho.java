package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import br.com.superauto.crm.relacionamento.entidade.Aniversario;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;
import br.com.superauto.crm.relacionamento.entidade.PeriodoDeAniversarios;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** FIL-04: consolida as placas elegíveis em uma linha por pessoa, sem depender de Spring. */
public final class SelecaoDeTrabalho {
    public List<LinhaDeTrabalho> aniversarios(
            List<ContratoNaFila> contratos, PeriodoDeAniversarios periodo, Set<Long> enviadas) {
        return contratos.stream()
                .collect(Collectors.groupingBy(ContratoNaFila::getAssociadoId))
                .values()
                .stream()
                .map(grupo -> aniversarioDaPessoa(grupo, periodo, enviadas))
                .sorted(
                        Comparator.comparing(LinhaDeTrabalho::data)
                                .thenComparing(LinhaDeTrabalho::nome))
                .toList();
    }

    private LinhaDeTrabalho aniversarioDaPessoa(
            List<ContratoNaFila> contratos, PeriodoDeAniversarios periodo, Set<Long> enviadas) {
        var primeiro = contratos.getFirst();
        var linha =
                LinhaDeTrabalho.de(
                        primeiro,
                        new ExibicaoNaFila(
                                Aniversario.proximo(primeiro.getNascimento(), periodo.hoje()),
                                periodo.hoje(),
                                enviadas.contains(primeiro.getAssociadoId())));
        return new LinhaDeTrabalho(
                linha.contratoId(),
                linha.associadoId(),
                linha.nome(),
                linha.whatsapp(),
                linha.aceitaWhatsapp(),
                contratos.stream()
                        .map(ContratoNaFila::getModelo)
                        .distinct()
                        .collect(Collectors.joining(" / ")),
                contratos.stream()
                        .map(c -> c.getPlaca().valor())
                        .collect(Collectors.joining(" / ")),
                contratos.stream()
                        .map(ContratoNaFila::getTipoVeiculo)
                        .distinct()
                        .collect(Collectors.joining(" / ")),
                contratos.stream()
                        .map(ContratoNaFila::getMensalidade)
                        .map(Mensalidade::valor)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                linha.cidade(),
                linha.situacao(),
                null,
                linha.data(),
                0,
                linha.versao(),
                linha.mensagemEnviada());
    }
}
