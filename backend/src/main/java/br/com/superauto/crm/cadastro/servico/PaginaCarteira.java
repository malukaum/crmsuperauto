package br.com.superauto.crm.cadastro.servico;

import java.math.BigDecimal;
import java.util.List;

/**
 * Uma página da carteira e o rodapé do filtro inteiro (CAR-01).
 *
 * @param somaMensalidades valor contratado, não recebido (RG-09)
 */
public record PaginaCarteira(List<LinhaDaCarteira> linhas, int pagina, int tamanho, long totalContratos,
                             BigDecimal somaMensalidades) {
}
