package br.com.superauto.crm.relacionamento.infraestrutura.persistencia;

import br.com.superauto.crm.config.FuncoesDeBusca;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;
import br.com.superauto.crm.relacionamento.entidade.LinhaHistoricoAtendimento;
import br.com.superauto.crm.relacionamento.repositorio.AutorDeAtendimento;
import br.com.superauto.crm.relacionamento.repositorio.ConsultaHistorico;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** ATD-05 e RG-03/RG-05: filtros, escopo do vendedor e busca executados no banco. */
@Component
@RequiredArgsConstructor
public class ConsultaHistoricoJpa implements ConsultaHistorico {
    private static final int MINIMO_PARA_BUSCAR_PLACA = 3;
    private final EntityManager em;

    @Override
    public List<LinhaHistoricoAtendimento> listar(Pedido pedido) {
        var cb = em.getCriteriaBuilder();
        var consulta = cb.createQuery(LinhaHistoricoAtendimento.class);
        var linha = consulta.from(LinhaHistoricoAtendimento.class);
        consulta.select(linha)
                .where(condicoes(cb, consulta, linha, pedido))
                .orderBy(cb.desc(linha.get("realizadoEm")), cb.desc(linha.get("id")));
        return em.createQuery(consulta).getResultList();
    }

    @Override
    public List<AutorDeAtendimento> autores(Optional<UUID> vendedor) {
        var cb = em.getCriteriaBuilder();
        var consulta = cb.createQuery(AutorDeAtendimento.class);
        var linha = consulta.from(LinhaHistoricoAtendimento.class);
        List<Predicate> regras = new ArrayList<>();
        vendedor.ifPresent(id -> regras.add(doVendedor(cb, consulta, linha, id)));
        consulta.select(cb.construct(AutorDeAtendimento.class, linha.get("registradoPorId"), linha.get("registradoPor")))
                .distinct(true)
                .where(regras.toArray(Predicate[]::new))
                .orderBy(cb.asc(linha.get("registradoPor")));
        return em.createQuery(consulta).getResultList();
    }

    private static Predicate[] condicoes(
            CriteriaBuilder cb, AbstractQuery<?> consulta, Root<LinhaHistoricoAtendimento> linha, Pedido pedido) {
        var filtro = pedido.filtro();
        List<Predicate> regras = new ArrayList<>();
        regras.add(cb.greaterThanOrEqualTo(linha.get("realizadoEm"), pedido.inicio()));
        regras.add(cb.lessThan(linha.get("realizadoEm"), pedido.fim()));
        pedido.vendedor().ifPresent(id -> regras.add(doVendedor(cb, consulta, linha, id)));
        filtro.tipo().ifPresent(tipo -> regras.add(cb.equal(linha.get("tipoCodigo"), tipo)));
        filtro.resultado().ifPresent(resultado -> regras.add(cb.equal(linha.get("resultado"), resultado)));
        filtro.usuario().ifPresent(usuario -> regras.add(cb.equal(linha.get("registradoPorId"), usuario)));
        filtro.cidade().ifPresent(cidade -> regras.add(cb.equal(linha.get("municipioId"), cidade)));
        filtro.busca().ifPresent(termo -> regras.add(busca(cb, linha, termo)));
        return regras.toArray(Predicate[]::new);
    }

    /** Atendimentos dos contratos do vendedor e contatos sem contrato sobre clientes dele. */
    private static Predicate doVendedor(
            CriteriaBuilder cb, AbstractQuery<?> consulta, Root<LinhaHistoricoAtendimento> linha, UUID vendedor) {
        var subconsulta = consulta.subquery(Long.class);
        var contrato = subconsulta.from(ContratoNaFila.class);
        subconsulta.select(contrato.get("contratoId"))
                .where(
                        cb.equal(contrato.get("vendedorId"), vendedor),
                        cb.equal(contrato.get("associadoId"), linha.get("associadoId")),
                        cb.or(
                                cb.isNull(linha.get("contratoId")),
                                cb.equal(contrato.get("contratoId"), linha.get("contratoId"))));
        return cb.exists(subconsulta);
    }

    /** Nome do cliente sem acento (RG-05) ou trecho da placa, com ou sem hífen. */
    private static Predicate busca(CriteriaBuilder cb, Root<LinhaHistoricoAtendimento> linha, String termo) {
        var normalizado = cb.function(FuncoesDeBusca.TEXTO_BUSCA, String.class, cb.literal(termo));
        List<Predicate> opcoes = new ArrayList<>();
        opcoes.add(cb.like(
                cb.function(FuncoesDeBusca.TEXTO_BUSCA, String.class, linha.get("associadoNome")),
                cb.concat(cb.concat("%", normalizado), "%")));
        String placa = termo.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (placa.length() >= MINIMO_PARA_BUSCAR_PLACA) {
            opcoes.add(cb.like(linha.get("placa"), "%" + placa + "%"));
        }
        return cb.or(opcoes.toArray(Predicate[]::new));
    }
}
