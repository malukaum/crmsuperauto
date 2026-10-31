package br.com.superauto.crm.cadastro.repositorio;

import br.com.superauto.crm.cadastro.entidade.LinhaCarteira;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import org.springframework.data.domain.Page;

/** Uma página da carteira e o rodapé do filtro inteiro: quantidade de contratos e soma das mensalidades (RG-09). */
public record ResultadoCarteira(Page<LinhaCarteira> pagina, Mensalidade somaMensalidades) {
}
