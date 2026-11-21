package br.com.superauto.crm.relacionamento.repositorio;

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

    List<LinhaHistoricoAtendimento> listar(Pedido pedido);

    /** Quem registrou atendimentos visíveis para o vendedor (ou para todos), para o filtro de usuário. */
    List<AutorDeAtendimento> autores(Optional<UUID> vendedor);
}
