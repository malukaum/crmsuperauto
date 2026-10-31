package br.com.superauto.crm.relacionamento.servico;

import java.util.Optional;
import java.util.UUID;

import br.com.superauto.crm.compartilhado.entidade.Fila;
import br.com.superauto.crm.relacionamento.repositorio.PosicaoFilaRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quantidades exibidas ao lado de "Contatos de hoje" e "Atrasados" no menu (ACE-02).
 * Vêm da visão das filas, que já exclui contratos suspensos e cancelados e usa o "hoje" de São Paulo.
 * O vendedor só conta os contratos em que é o vendedor (RG-03).
 */
@Service
public class ContadoresFilasService {

    private final PosicaoFilaRepository filas;

    public ContadoresFilasService(PosicaoFilaRepository filas) {
        this.filas = filas;
    }

    public record Contadores(long contatosHoje, long atrasados) {
    }

    /** @param vendedor preenchido só quando quem pergunta é vendedor (escopo da carteira dele) */
    @Transactional(readOnly = true)
    public Contadores contar(Optional<UUID> vendedor) {
        return new Contadores(contar(Fila.HOJE, vendedor), contar(Fila.ATRASADO, vendedor));
    }

    private long contar(Fila fila, Optional<UUID> vendedor) {
        return vendedor.map(v -> filas.countByFilaAndVendedorId(fila, v)).orElseGet(() -> filas.countByFila(fila));
    }
}
