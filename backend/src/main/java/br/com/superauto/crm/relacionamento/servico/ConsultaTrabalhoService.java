package br.com.superauto.crm.relacionamento.servico;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;
import br.com.superauto.crm.relacionamento.entidade.PeriodoDeAniversarios;
import br.com.superauto.crm.relacionamento.entidade.TipoDeRelacionamento;
import br.com.superauto.crm.relacionamento.repositorio.AtendimentoRepository;
import br.com.superauto.crm.relacionamento.repositorio.ConsultaFilas;
import br.com.superauto.crm.relacionamento.repositorio.FiltroDaFila;
import br.com.superauto.crm.relacionamento.repositorio.PeriodoDeAtendimentos;
import br.com.superauto.crm.relacionamento.repositorio.TiposDeContato;
import br.com.superauto.crm.relacionamento.repositorio.TrabalhoRelacionamentoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** FIL-01 a FIL-04: orquestra consultas com escopo; critérios e agregações executam no banco. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsultaTrabalhoService {
    private static final Pageable DESTAQUES_DO_PAINEL = PageRequest.of(0, 5);
    private static final int PROXIMOS_SETE_DIAS = 7;
    private final TrabalhoRelacionamentoRepository trabalho;
    private final ConsultaFilas filas;
    private final AtendimentoRepository atendimentos;
    private final TiposDeContato tipos;
    private final SelecaoDeTrabalho selecao;
    private final IndicadoresDaCarteira indicadores;
    private final Clock relogio;

    public Page<LinhaDeTrabalho> contatos(FilaSolicitada fila, Pageable pagina) {
        var pedido = new ConsultaFilas.Contatos(
                fila.atrasados(), LocalDate.now(relogio), fila.filtro(), fila.vendedor());
        return linhas(filas.contatos(pedido, pagina), pedido.hoje());
    }

    /**
     * FIL-04 · Uma página de aniversariantes. A página é montada depois de juntar as placas de cada pessoa numa linha
     * só, já que a consulta traz uma linha por contrato; a janela é de no máximo 30 dias.
     */
    public Page<LinhaDeTrabalho> aniversarios(int dias, Optional<UUID> vendedor, Pageable pagina) {
        var pessoas = aniversariosDoPeriodo(dias, vendedor);
        int inicio = (int) Math.min(pagina.getOffset(), pessoas.size());
        int fim = Math.min(inicio + pagina.getPageSize(), pessoas.size());
        return new PageImpl<>(pessoas.subList(inicio, fim), pagina, pessoas.size());
    }

    private List<LinhaDeTrabalho> aniversariosDoPeriodo(int dias, Optional<UUID> vendedor) {
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

    public ResumoDoPainel painel(Optional<UUID> vendedor) {
        var hoje = LocalDate.now(relogio);
        var carteira = indicadores.consolidar(trabalho.contagens(vendedor.orElse(null)));
        var pedidoHoje =
                new ConsultaFilas.Contatos(false, hoje, FiltroDaFila.SEM_FILTROS, vendedor);
        var pedidoAtrasados =
                new ConsultaFilas.Contatos(true, hoje, FiltroDaFila.SEM_FILTROS, vendedor);
        var aniversarios = aniversariosDoPeriodo(PROXIMOS_SETE_DIAS, vendedor);
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
                linhas(filas.contatos(pedidoHoje, DESTAQUES_DO_PAINEL), hoje).getContent(),
                aniversarios.stream().limit(DESTAQUES_DO_PAINEL.getPageSize()).toList());
    }

    private static Page<LinhaDeTrabalho> linhas(Page<ContratoNaFila> contratos, LocalDate hoje) {
        return contratos.map(c -> LinhaDeTrabalho.de(c, new ExibicaoNaFila(c.getProximoContato(), hoje, false)));
    }
}
