package br.com.superauto.crm.relacionamento.infraestrutura.persistencia;

import br.com.superauto.crm.config.FuncoesDeBusca;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;
import br.com.superauto.crm.relacionamento.entidade.PeriodoDeAniversarios;
import br.com.superauto.crm.relacionamento.entidade.SituacaoNaCarteira;
import br.com.superauto.crm.relacionamento.repositorio.ConsultaFilas;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import lombok.RequiredArgsConstructor;

import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.stereotype.Component;

import java.time.MonthDay;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** RG-03 e RG-05: critérios, busca e ordenação executados no banco, antes de carregar as linhas. */
@Component
@RequiredArgsConstructor
public class ConsultaFilasJpa implements ConsultaFilas {
    private final EntityManager em;

    public List<ContratoNaFila> contatos(Contatos pedido, int limite) {
        var cb = (HibernateCriteriaBuilder) em.getCriteriaBuilder();
        var consulta = cb.createQuery(ContratoNaFila.class);
        var contrato = consulta.from(ContratoNaFila.class);
        consulta.select(contrato).where(condicoes(cb, contrato, pedido));
        List<Order> ordem = new ArrayList<>();
        if (pedido.atrasados()) ordem.add(cb.asc(contrato.get("proximoContato")));
        ordem.add(cb.asc(contrato.get("nome")));
        ordem.add(cb.asc(contrato.get("contratoId")));
        var busca = em.createQuery(consulta.orderBy(ordem));
        if (limite > 0) busca.setMaxResults(limite);
        return busca.getResultList();
    }

    public long contarContatos(Contatos pedido) {
        var cb = (HibernateCriteriaBuilder) em.getCriteriaBuilder();
        var consulta = cb.createQuery(Long.class);
        var contrato = consulta.from(ContratoNaFila.class);
        return em.createQuery(
                        consulta.select(cb.count(contrato)).where(condicoes(cb, contrato, pedido)))
                .getSingleResult();
    }

    public List<ContratoNaFila> aniversarios(
            PeriodoDeAniversarios periodo, Optional<UUID> vendedor) {
        var cb = (HibernateCriteriaBuilder) em.getCriteriaBuilder();
        var consulta = cb.createQuery(ContratoNaFila.class);
        var contrato = consulta.from(ContratoNaFila.class);
        var datas =
                periodo.nascimentosElegiveis().stream()
                        .map(data -> nascimento(cb, contrato, data))
                        .toArray(Predicate[]::new);
        consulta.select(contrato)
                .where(cb.and(elegivel(cb, contrato, vendedor), cb.or(datas)))
                .orderBy(cb.asc(contrato.get("nome")), cb.asc(contrato.get("contratoId")));
        return em.createQuery(consulta).getResultList();
    }

    private static Predicate[] condicoes(
            CriteriaBuilder cb, Root<ContratoNaFila> contrato, Contatos pedido) {
        List<Predicate> regras = new ArrayList<>();
        var filtro = pedido.filtro();
        regras.add(elegivel(cb, contrato, pedido.vendedor()));
        regras.add(cb.isNotNull(contrato.get("etapaId")));
        regras.add(
                pedido.atrasados()
                        ? cb.lessThan(contrato.get("proximoContato"), pedido.hoje())
                        : cb.equal(contrato.get("proximoContato"), pedido.hoje()));
        if (filtro.cidade() != null)
            regras.add(cb.equal(contrato.get("municipioId"), filtro.cidade()));
        if (filtro.etapa() != null) regras.add(cb.equal(contrato.get("etapaId"), filtro.etapa()));
        if (filtro.tipoVeiculo() != null)
            regras.add(cb.equal(contrato.get("tipoVeiculoId"), filtro.tipoVeiculo()));
        if (filtro.busca() != null && !filtro.busca().isBlank())
            regras.add(busca(cb, contrato, filtro.busca()));
        return regras.toArray(Predicate[]::new);
    }

    private static Predicate elegivel(
            CriteriaBuilder cb, Root<ContratoNaFila> contrato, Optional<UUID> vendedor) {
        var situacao =
                contrato.get("situacao")
                        .in(SituacaoNaCarteira.A_CONFERIR, SituacaoNaCarteira.ATIVO);
        return vendedor.map(id -> cb.and(situacao, cb.equal(contrato.get("vendedorId"), id)))
                .orElse(situacao);
    }

    private static Predicate nascimento(
            HibernateCriteriaBuilder cb, Root<ContratoNaFila> contrato, MonthDay data) {
        return cb.and(
                cb.equal(cb.month(contrato.get("nascimento")), data.getMonthValue()),
                cb.equal(cb.day(contrato.get("nascimento")), data.getDayOfMonth()));
    }

    private static Predicate busca(
            CriteriaBuilder cb, Root<ContratoNaFila> contrato, String termo) {
        var normalizado =
                cb.function(FuncoesDeBusca.TEXTO_BUSCA, String.class, cb.literal(termo.strip()));
        List<Predicate> opcoes = new ArrayList<>();
        opcoes.add(
                cb.like(
                        cb.function(FuncoesDeBusca.TEXTO_BUSCA, String.class, contrato.get("nome")),
                        cb.concat(cb.concat("%", normalizado), "%")));
        String codigo = termo.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT),
                digitos = termo.replaceAll("\\D", "");
        if (codigo.length() >= 3) {
            opcoes.add(cb.like(contrato.get("placa").as(String.class), "%" + codigo + "%"));
            opcoes.add(cb.like(contrato.get("documento").as(String.class), "%" + codigo + "%"));
        }
        if (digitos.length() >= 3)
            opcoes.add(cb.like(contrato.get("telefones"), "%" + digitos + "%"));
        return cb.or(opcoes.toArray(Predicate[]::new));
    }
}
