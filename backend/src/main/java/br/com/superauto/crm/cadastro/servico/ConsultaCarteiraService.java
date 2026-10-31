package br.com.superauto.crm.cadastro.servico;

import br.com.superauto.crm.cadastro.repositorio.ConsultaCarteira;
import br.com.superauto.crm.cadastro.repositorio.FiltroCarteira;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CAR-01 · Buscar e filtrar a carteira. O vendedor só vê os próprios contratos, qualquer que seja o filtro (RG-03). */
@Service
@Transactional(readOnly = true)
public class ConsultaCarteiraService {

    private final ConsultaCarteira carteira;

    public ConsultaCarteiraService(ConsultaCarteira carteira) {
        this.carteira = carteira;
    }

    public PaginaCarteira buscar(FiltroCarteira filtro, PaginaSolicitada pagina, EscopoCarteira escopo) {
        var resultado = carteira.buscar(noEscopo(filtro, escopo), paginacao(pagina));
        var linhas = resultado.pagina().getContent().stream().map(LinhaDaCarteira::de).toList();
        return new PaginaCarteira(linhas, pagina.numero(), pagina.tamanho(), resultado.pagina().getTotalElements(),
                resultado.somaMensalidades().valor());
    }

    static Sort ordem(Ordenacao ordenacao) {
        return Sort.by(ordenacao.decrescente() ? Sort.Direction.DESC : Sort.Direction.ASC, ordenacao.coluna().atributo());
    }

    private static Pageable paginacao(PaginaSolicitada pagina) {
        return PageRequest.of(pagina.numero(), pagina.tamanho(), ordem(pagina.ordenacao()));
    }

    static FiltroCarteira noEscopo(FiltroCarteira filtro, EscopoCarteira escopo) {
        return escopo.vendedor().map(filtro::doVendedor).orElse(filtro);
    }
}
