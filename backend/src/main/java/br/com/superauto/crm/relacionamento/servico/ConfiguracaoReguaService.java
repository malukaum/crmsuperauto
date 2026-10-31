package br.com.superauto.crm.relacionamento.servico;

import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.relacionamento.entidade.AcompanhamentoContrato;
import br.com.superauto.crm.relacionamento.entidade.ContratoNaFila;
import br.com.superauto.crm.relacionamento.entidade.EtapaRegua;
import br.com.superauto.crm.relacionamento.entidade.PrazosDaRegua;
import br.com.superauto.crm.relacionamento.repositorio.AcompanhamentoContratoRepository;
import br.com.superauto.crm.relacionamento.repositorio.ConfiguracaoDosPrazos;
import br.com.superauto.crm.relacionamento.repositorio.ReguaDePosVenda;
import br.com.superauto.crm.relacionamento.repositorio.TrabalhoRelacionamentoRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** FIL-06: prévia identificada pelo estado lido; mudanças posteriores exigem uma nova prévia. */
@Service
@RequiredArgsConstructor
public class ConfiguracaoReguaService {
    private final ReguaDePosVenda regua;
    private final ConfiguracaoDosPrazos configuracao;
    private final TrabalhoRelacionamentoRepository trabalho;
    private final AcompanhamentoContratoRepository acompanhamentos;

    @Transactional(readOnly = true)
    public List<EtapaRegua> etapas() {
        return regua.etapas();
    }

    @Transactional(readOnly = true)
    public PreviaDaRegua prever(Map<Short, Integer> prazos) {
        return plano(prazos).previa();
    }

    @Transactional
    public void salvar(Map<Short, Integer> prazos, String confirmacao, UUID autor) {
        Plano plano = plano(prazos);
        if (!plano.previa().confirmacao().equals(confirmacao)) {
            throw NegocioException.conflito(
                    "previa-desatualizada",
                    "A carteira ou a régua mudou. Gere uma nova prévia antes de confirmar.");
        }
        configuracao.alterarPrazos(prazos, autor);
        plano.acompanhamentos()
                .forEach(
                        acompanhamento ->
                                acompanhamento.recalcularAgenda(
                                        plano.contratos()
                                                .get(acompanhamento.getId())
                                                .getBaseRegua(),
                                        plano.etapas(),
                                        autor));
        acompanhamentos.saveAll(plano.acompanhamentos());
    }

    private Plano plano(Map<Short, Integer> prazos) {
        var atuais = regua.etapas();
        var novas = new PrazosDaRegua(prazos).aplicar(atuais);
        Map<Long, ContratoNaFila> contratos = new LinkedHashMap<>();
        trabalho.comReguaAtiva().forEach(c -> contratos.put(c.getContratoId(), c));
        var ativos =
                acompanhamentos.findAllById(contratos.keySet()).stream()
                        .filter(
                                a ->
                                        a.getEtapaPendenteId().isPresent()
                                                && contratos.containsKey(a.getId()))
                        .sorted(Comparator.comparing(AcompanhamentoContrato::getId))
                        .toList();
        return new Plano(atuais, novas, ativos, contratos);
    }

    private record Plano(
            List<EtapaRegua> anteriores,
            List<EtapaRegua> etapas,
            List<AcompanhamentoContrato> acompanhamentos,
            Map<Long, ContratoNaFila> contratos) {
        PreviaDaRegua previa() {
            long quantidade = acompanhamentos.stream().filter(this::alteraData).count();
            String estado =
                    anteriores.toString()
                            + etapas
                            + acompanhamentos.stream().map(this::identificar).toList();
            return new PreviaDaRegua(quantidade, assinatura(estado));
        }

        private boolean alteraData(AcompanhamentoContrato acompanhamento) {
            var base = contratos.get(acompanhamento.getId()).getBaseRegua();
            return !acompanhamento
                    .getProximoContatoEm()
                    .equals(acompanhamento.preverRecalculo(base, etapas));
        }

        private String identificar(AcompanhamentoContrato acompanhamento) {
            return acompanhamento.getId()
                    + ":"
                    + acompanhamento.getVersao()
                    + ":"
                    + contratos.get(acompanhamento.getId()).getBaseRegua();
        }
    }

    private static String assinatura(String estado) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(estado.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
