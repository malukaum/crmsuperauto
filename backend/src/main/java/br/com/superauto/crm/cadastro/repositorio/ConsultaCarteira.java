package br.com.superauto.crm.cadastro.repositorio;

import java.util.List;

import br.com.superauto.crm.cadastro.entidade.LinhaCarteira;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Leitura da carteira (CAR-01, CAR-07): filtros dinâmicos sobre o modelo de leitura {@link LinhaCarteira}. */
public interface ConsultaCarteira {

    ResultadoCarteira buscar(FiltroCarteira filtro, Pageable pagina);

    /** A lista inteira do filtro, para a exportação. */
    List<LinhaCarteira> listar(FiltroCarteira filtro, Sort ordem);

    /** As linhas (contratos) de um associado, para a ficha. */
    List<LinhaCarteira> doAssociado(Long associadoId);
}
