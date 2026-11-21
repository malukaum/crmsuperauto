package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.relacionamento.repositorio.ConsultaFilas;
import br.com.superauto.crm.relacionamento.repositorio.FiltroDaFila;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Quantidades exibidas ao lado de "Contatos de hoje" e "Atrasados" no menu (ACE-02, FIL-02).
 * Usam o mesmo critério das filas e o "hoje" do relógio de São Paulo (RG-01), para o contador
 * nunca divergir da lista. O vendedor só conta os contratos em que é o vendedor (RG-03).
 */
@Service
@RequiredArgsConstructor
public class ContadoresFilasService {
    private final ConsultaFilas filas;
    private final Clock relogio;

    public record Contadores(long contatosHoje, long atrasados) {}

    /** @param vendedor preenchido só quando quem pergunta é vendedor (escopo da carteira dele) */
    @Transactional(readOnly = true)
    public Contadores contar(Optional<UUID> vendedor) {
        var hoje = LocalDate.now(relogio);
        return new Contadores(
                contar(false, hoje, vendedor), contar(true, hoje, vendedor));
    }

    private long contar(boolean atrasados, LocalDate hoje, Optional<UUID> vendedor) {
        return filas.contarContatos(
                new ConsultaFilas.Contatos(atrasados, hoje, FiltroDaFila.SEM_FILTROS, vendedor));
    }
}
