package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.relacionamento.entidade.Atendimento;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;
import br.com.superauto.crm.relacionamento.entidade.MomentoDoAtendimento;
import br.com.superauto.crm.relacionamento.repositorio.AtendimentoRepository;
import br.com.superauto.crm.relacionamento.repositorio.TiposDeContato;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZonedDateTime;

/**
 * ATD-02 · Registrar atendimento avulso. Nunca mexe na régua, pode ser sobre a pessoa ou sobre um
 * contrato, inclusive cancelado, para a reaproximação de ex-clientes.
 */
@Service
@RequiredArgsConstructor
public class AtendimentoAvulsoService {
    private final CarteiraVisivel carteira;
    private final AtendimentoRepository atendimentos;
    private final TiposDeContato tipos;
    private final Clock relogio;

    /** @return o id do atendimento gravado, para anexar anotações a ele (IA-01) */
    @Transactional
    public Long registrar(RegistroAvulso registro, AutorDoContato autor) {
        registro.tipo().exigirAvulso();
        var contratos = carteira.contratosDoAssociado(registro.associadoId(), autor.vendedor());
        registro.contratoId()
                .filter(id -> contratos.stream().map(ContratoNaFila::getContratoId).noneMatch(id::equals))
                .ifPresent(
                        id -> {
                            throw NegocioException.naoEncontrado("Contrato não encontrado.");
                        });
        var momento = MomentoDoAtendimento.de(registro.realizadoEm(), ZonedDateTime.now(relogio));
        return atendimentos.save(
                Atendimento.registrar(
                        new Atendimento.Novo(
                                registro.associadoId(),
                                registro.contratoId().orElse(null),
                                tipos.id(registro.tipo()),
                                null,
                                registro.resultado(),
                                registro.descricao(),
                                registro.retorno().orElse(null),
                                momento.valor(),
                                autor.id())))
                .getId();
    }
}
