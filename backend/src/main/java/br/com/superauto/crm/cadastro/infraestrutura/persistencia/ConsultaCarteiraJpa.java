package br.com.superauto.crm.cadastro.infraestrutura.persistencia;

import br.com.superauto.crm.cadastro.entidade.LinhaCarteira;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.cadastro.repositorio.ConsultaCarteira;
import br.com.superauto.crm.cadastro.repositorio.FiltroCarteira;
import br.com.superauto.crm.cadastro.repositorio.OrdemCarteira;
import br.com.superauto.crm.cadastro.repositorio.ResultadoCarteira;
import br.com.superauto.crm.cadastro.repositorio.TermoBusca;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import br.com.superauto.crm.config.FuncoesDeBusca;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Consulta da carteira com a Criteria API do JPA (sem SQL nativo): os filtros são opcionais e combinados com "e";
 * a busca (RG-05) procura o termo no nome sem acento ({@code texto_busca}), na placa, no documento e nos telefones.
 */
@Component
public class ConsultaCarteiraJpa implements ConsultaCarteira {

    private final EntityManager em;

    public ConsultaCarteiraJpa(EntityManager em) {
        this.em = em;
    }

    @Override
    public ResultadoCarteira buscar(FiltroCarteira filtro, Pageable pagina) {
        List<LinhaCarteira> linhas = em.createQuery(selecao(filtro, pagina.getSort()))
                .setFirstResult((int) pagina.getOffset())
                .setMaxResults(pagina.getPageSize())
                .getResultList();
        return new ResultadoCarteira(new PageImpl<>(linhas, pagina, contar(filtro)), somar(filtro));
    }

    @Override
    public List<LinhaCarteira> listar(FiltroCarteira filtro, Sort ordem) {
        return em.createQuery(selecao(filtro, ordem)).getResultList();
    }

    @Override
    public List<LinhaCarteira> doAssociado(Long associadoId) {
        return em.createQuery("""
                        select l from LinhaCarteira l
                         where l.associadoId = :associadoId
                         order by l.aderidoEm, l.contratoId
                        """, LinhaCarteira.class)
                .setParameter("associadoId", associadoId)
                .getResultList();
    }

    private CriteriaQuery<LinhaCarteira> selecao(FiltroCarteira filtro, Sort ordem) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<LinhaCarteira> consulta = cb.createQuery(LinhaCarteira.class);
        Root<LinhaCarteira> linha = consulta.from(LinhaCarteira.class);
        return consulta.select(linha).where(condicoes(cb, linha, filtro)).orderBy(ordenacao(cb, linha, ordem));
    }

    private long contar(FiltroCarteira filtro) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> consulta = cb.createQuery(Long.class);
        Root<LinhaCarteira> linha = consulta.from(LinhaCarteira.class);
        return em.createQuery(consulta.select(cb.count(linha)).where(condicoes(cb, linha, filtro))).getSingleResult();
    }

    private Mensalidade somar(FiltroCarteira filtro) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> consulta = cb.createQuery(BigDecimal.class);
        Root<LinhaCarteira> linha = consulta.from(LinhaCarteira.class);
        Expression<BigDecimal> mensalidade = linha.get("mensalidade").as(BigDecimal.class);
        consulta.select(cb.coalesce(cb.sum(mensalidade), BigDecimal.ZERO)).where(condicoes(cb, linha, filtro));
        return new Mensalidade(em.createQuery(consulta).getSingleResult());
    }

    private static Predicate[] condicoes(CriteriaBuilder cb, Root<LinhaCarteira> linha, FiltroCarteira filtro) {
        List<Predicate> condicoes = new ArrayList<>();
        if (filtro.excluirCancelados()) {
            condicoes.add(cb.notEqual(linha.get("situacao"), SituacaoContrato.CANCELADO));
        }
        filtro.busca().ifPresent(termo -> condicoes.add(busca(cb, linha, termo)));
        filtro.fila().ifPresent(f -> condicoes.add(cb.equal(linha.get("fila"), f)));
        filtro.municipioId().ifPresent(m -> condicoes.add(cb.equal(linha.get("municipioId"), m)));
        filtro.situacao().ifPresent(s -> condicoes.add(cb.equal(linha.get("situacao"), s)));
        filtro.etapaId().ifPresent(e -> condicoes.add(cb.equal(linha.get("etapaPendenteId"), e)));
        filtro.tipoVeiculoId().ifPresent(t -> condicoes.add(cb.equal(linha.get("tipoVeiculoId"), t)));
        filtro.perfil().ifPresent(p -> condicoes.add(cb.equal(linha.get("perfil"), p)));
        filtro.potencial().ifPresent(p -> condicoes.add(cb.equal(linha.get("potencialIndicacao"), p)));
        filtro.vendedorId().ifPresent(v -> condicoes.add(cb.equal(linha.get("vendedorId"), v)));
        return condicoes.toArray(Predicate[]::new);
    }

    private static Predicate busca(CriteriaBuilder cb, Root<LinhaCarteira> linha, TermoBusca termo) {
        List<Predicate> alternativas = new ArrayList<>();
        alternativas.add(cb.like(semAcento(cb, linha.get("associadoNome")),
                cb.concat(cb.concat("%", semAcento(cb, cb.literal(termo.texto()))), "%")));
        termo.alfanumerico().ifPresent(codigo -> {
            alternativas.add(cb.like(linha.get("placa").as(String.class), contendo(codigo)));
            alternativas.add(cb.like(linha.get("documento").as(String.class), contendo(codigo)));
        });
        termo.digitos().ifPresent(digitos -> alternativas.add(cb.like(linha.get("telefones"), contendo(digitos))));
        return cb.or(alternativas.toArray(Predicate[]::new));
    }

    private static Expression<String> semAcento(CriteriaBuilder cb, Expression<String> texto) {
        return cb.function(FuncoesDeBusca.TEXTO_BUSCA, String.class, texto);
    }

    private static String contendo(String trecho) {
        return "%" + trecho.replace("%", "").replace("_", "") + "%";
    }

    /** A ordem pedida e, para desempate estável entre páginas, o nome e o contrato. */
    private static List<Order> ordenacao(CriteriaBuilder cb, Root<LinhaCarteira> linha, Sort ordem) {
        List<Order> ordens = new ArrayList<>();
        ordem.forEach(o -> ordens.add(o.isAscending() ? cb.asc(linha.get(o.getProperty())) : cb.desc(linha.get(o.getProperty()))));
        String nome = OrdemCarteira.NOME.atributo();
        if (ordem.getOrderFor(nome) == null) {
            ordens.add(cb.asc(linha.get(nome)));
        }
        ordens.add(cb.asc(linha.get("contratoId")));
        return ordens;
    }
}
