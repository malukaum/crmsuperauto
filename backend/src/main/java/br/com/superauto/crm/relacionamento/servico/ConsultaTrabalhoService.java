package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;
import br.com.superauto.crm.relacionamento.entidade.LinhaHistoricoAtendimento;
import br.com.superauto.crm.relacionamento.entidade.PeriodoDeAniversarios;
import br.com.superauto.crm.relacionamento.entidade.TipoDeRelacionamento;
import br.com.superauto.crm.relacionamento.repositorio.AtendimentoRepository;
import br.com.superauto.crm.relacionamento.repositorio.ConsultaFilas;
import br.com.superauto.crm.relacionamento.repositorio.FiltroDaFila;
import br.com.superauto.crm.relacionamento.repositorio.HistoricoAtendimentoRepository;
import br.com.superauto.crm.relacionamento.repositorio.PeriodoDeAtendimentos;
import br.com.superauto.crm.relacionamento.repositorio.TiposDeContato;
import br.com.superauto.crm.relacionamento.repositorio.TrabalhoRelacionamentoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** FIL-01 a FIL-04: orquestra consultas com escopo; critérios e agregações executam no banco. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaTrabalhoService {
    private static final int SEM_LIMITE = 0;
    private static final int MAXIMO_DE_DESTAQUES = 5;
    private static final int PROXIMOS_SETE_DIAS = 7;
    private final TrabalhoRelacionamentoRepository trabalho;
    private final ConsultaFilas filas;
    private final AtendimentoRepository atendimentos;
    private final HistoricoAtendimentoRepository historico;
    private final TiposDeContato tipos;
    private final SelecaoDeTrabalho selecao;
    private final IndicadoresDaCarteira indicadores;
    private final Clock relogio;

    public List<LinhaDeTrabalho> contatos(
            boolean atrasados, FiltroDaFila filtro, Optional<UUID> vendedor) {
        var pedido =
                new ConsultaFilas.Contatos(atrasados, LocalDate.now(relogio), filtro, vendedor);
        return linhas(filas.contatos(pedido, SEM_LIMITE), pedido.hoje());
    }

    public List<LinhaDeTrabalho> aniversarios(int dias, Optional<UUID> vendedor) {
        var periodo = new PeriodoDeAniversarios(LocalDate.now(relogio), dias);
        return selecao.aniversarios(
                filas.aniversarios(periodo, vendedor), periodo, enviadas(periodo.hoje(), vendedor));
    }

    private Set<Long> enviadas(LocalDate hoje, Optional<UUID> vendedor) {
        var inicio = hoje.withDayOfYear(1).atStartOfDay(relogio.getZone());
        return atendimentos.aniversariosRegistrados(
                tipos.id(TipoDeRelacionamento.ANIVERSARIO),
                new PeriodoDeAtendimentos(
                        inicio.toOffsetDateTime(),
                        inicio.plusYears(1).toOffsetDateTime(),
                        vendedor));
    }

    public List<ContatoDoMes> atendimentosMes(Optional<UUID> vendedor) {
        var inicio = LocalDate.now(relogio).withDayOfMonth(1).atStartOfDay(relogio.getZone());
        var contatos =
                historico.noPeriodo(
                        inicio.toOffsetDateTime(),
                        inicio.plusMonths(1).toOffsetDateTime(),
                        vendedor.orElse(null));
        Set<Long> associados =
                contatos.stream()
                        .map(LinhaHistoricoAtendimento::getAssociadoId)
                        .collect(Collectors.toSet());
        Map<Long, String> nomes = new HashMap<>();
        if (!associados.isEmpty())
            trabalho.dosAssociados(associados, vendedor.orElse(null))
                    .forEach(c -> nomes.put(c.getAssociadoId(), c.getNome()));
        return contatos.stream().map(h -> contatoDoMes(h, nomes)).toList();
    }

    private ContatoDoMes contatoDoMes(LinhaHistoricoAtendimento contato, Map<Long, String> nomes) {
        return new ContatoDoMes(
                contato.getId(),
                contato.getAssociadoId(),
                nomes.getOrDefault(contato.getAssociadoId(), "Associado"),
                contato.getPlaca().orElse(null),
                contato.getTipo(),
                contato.getEtapa().orElse(null),
                contato.getResultado(),
                contato.getDescricao().orElse(null),
                contato.getRealizadoEm().atZoneSameInstant(relogio.getZone()).toOffsetDateTime(),
                contato.getRegistradoPor());
    }

    public ResumoDoPainel painel(Optional<UUID> vendedor) {
        var hoje = LocalDate.now(relogio);
        var carteira = indicadores.consolidar(trabalho.contagens(vendedor.orElse(null)));
        var pedidoHoje =
                new ConsultaFilas.Contatos(false, hoje, FiltroDaFila.SEM_FILTROS, vendedor);
        var pedidoAtrasados =
                new ConsultaFilas.Contatos(true, hoje, FiltroDaFila.SEM_FILTROS, vendedor);
        var aniversarios = aniversarios(PROXIMOS_SETE_DIAS, vendedor);
        var inicio = hoje.withDayOfMonth(1).atStartOfDay(relogio.getZone());
        return new ResumoDoPainel(
                carteira.carteira(),
                carteira.porTipo(),
                filas.contarContatos(pedidoHoje),
                filas.contarContatos(pedidoAtrasados),
                aniversarios.stream().filter(linha -> hoje.equals(linha.data())).count(),
                aniversarios.size(),
                carteira.mensalidadesAtivas(),
                atendimentos.contarNoPeriodo(
                        new PeriodoDeAtendimentos(
                                inicio.toOffsetDateTime(),
                                inicio.plusMonths(1).toOffsetDateTime(),
                                vendedor)),
                carteira.aConferir(),
                carteira.situacoes(),
                linhas(filas.contatos(pedidoHoje, MAXIMO_DE_DESTAQUES), hoje),
                aniversarios.stream().limit(MAXIMO_DE_DESTAQUES).toList());
    }

    private static List<LinhaDeTrabalho> linhas(List<ContratoNaFila> contratos, LocalDate hoje) {
        return contratos.stream()
                .map(
                        c ->
                                LinhaDeTrabalho.de(
                                        c, new ExibicaoNaFila(c.getProximoContato(), hoje, false)))
                .toList();
    }
}
