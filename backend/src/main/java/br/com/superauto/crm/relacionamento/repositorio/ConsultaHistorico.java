package br.com.superauto.crm.relacionamento.repositorio;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import br.com.superauto.crm.relacionamento.entidade.LinhaHistoricoAtendimento;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * ATD-05 · Consulta do histórico de atendimentos executada no banco, do mais recente para o mais antigo. O vendedor
 * vê os atendimentos dos seus contratos e os contatos sobre os seus clientes, como a mensagem de aniversário (RG-03).
 */
public interface ConsultaHistorico {
    /** @param fim exclusivo */
    record Pedido(
            OffsetDateTime inicio, OffsetDateTime fim, FiltroDoHistorico filtro, Optional<UUID> vendedor) {}

    /** Uma página do histórico, do mais recente para o mais antigo (a ordenação pedida pelo cliente é ignorada). */
    Page<LinhaHistoricoAtendimento> listar(Pedido pedido, Pageable pagina);

    /** O histórico inteiro do pedido, para a exportação. */
    List<LinhaHistoricoAtendimento> todos(Pedido pedido);

    /** Quem registrou atendimentos visíveis para o vendedor (ou para todos), para o filtro de usuário. */
    List<AutorDeAtendimento> autores(Optional<UUID> vendedor);
}
