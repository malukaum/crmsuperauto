package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.relacionamento.entidade.Correcao;
import br.com.superauto.crm.relacionamento.entidade.ResponsavelPelaCorrecao;
import br.com.superauto.crm.relacionamento.repositorio.AtendimentoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZonedDateTime;

/**
 * ATD-04 · Corrigir um atendimento: grava um novo registro que aponta para o original, que passa a aparecer como
 * corrigido. A régua não é desfeita; se precisar, o gerencial a ajusta na ficha.
 */
@Service
@RequiredArgsConstructor
public class CorrecaoDeAtendimentoService {
    private final AtendimentoRepository atendimentos;
    private final Clock relogio;

    @Transactional
    public void corrigir(Long atendimentoId, Correcao correcao, ResponsavelPelaCorrecao responsavel) {
        var original =
                atendimentos
                        .buscarParaAlteracao(atendimentoId)
                        .orElseThrow(() -> NegocioException.naoEncontrado("Atendimento não encontrado."));
        original.exigirSemCorrecao(atendimentos.existsByCorrigeAtendimentoId(atendimentoId));
        atendimentos.save(original.corrigir(correcao, responsavel, ZonedDateTime.now(relogio)));
    }
}
