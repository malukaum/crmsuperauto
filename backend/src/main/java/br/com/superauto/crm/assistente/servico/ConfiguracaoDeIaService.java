package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.ConfiguracaoDoProvedor;
import br.com.superauto.crm.assistente.entidade.ConfiguracaoDoProvedor.ChaveCifrada;
import br.com.superauto.crm.assistente.entidade.ProvedorDeIa;
import br.com.superauto.crm.assistente.entidade.TarefaDeIa;
import br.com.superauto.crm.assistente.repositorio.ClienteDeIa.AcessoAoProvedor;
import br.com.superauto.crm.assistente.repositorio.ClienteDeIa.FalhaNoProvedor;
import br.com.superauto.crm.assistente.repositorio.ClienteDeIa.PedidoDaIa;
import br.com.superauto.crm.assistente.repositorio.CofreDeChaves;
import br.com.superauto.crm.assistente.repositorio.ConfiguracaoDoProvedorRepository;
import br.com.superauto.crm.assistente.repositorio.RotaDaTarefaRepository;
import br.com.superauto.crm.assistente.repositorio.UsoDeIaRepository;
import br.com.superauto.crm.assistente.repositorio.UsoPorProvedor;
import br.com.superauto.crm.erro.NegocioException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * IA-05 · Configuração do assistente pelo gerencial: provedores (modelo, chave cifrada, ligado, limite, preços),
 * principal e reserva de cada tarefa, teste de conexão e uso do mês. Toda mudança vai para a auditoria (RG-08).
 */
@Service
@RequiredArgsConstructor
public class ConfiguracaoDeIaService {
    private static final PedidoDaIa PEDIDO_DE_TESTE =
            new PedidoDaIa("Responda apenas com a palavra OK.", "Teste de conexão.", 1_000);

    private final ConfiguracaoDoProvedorRepository configuracoes;
    private final RotaDaTarefaRepository rotas;
    private final UsoDeIaRepository usos;
    private final CofreDeChaves cofre;
    private final ExecutorDeIa executor;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public List<ProvedorConfigurado> provedores() {
        return configuracoes.findAll().stream()
                .sorted(Comparator.comparing(ConfiguracaoDoProvedor::getProvedor))
                .map(c -> ProvedorConfigurado.de(c, executor.gastoDoMes(c.getProvedor())))
                .toList();
    }

    @Transactional
    public ProvedorConfigurado ajustar(ProvedorDeIa provedor, AlteracaoDoProvedor alteracao, UUID autor) {
        var configuracao = configuracoes.buscarParaAlteracao(provedor)
                .orElseThrow(() -> NegocioException.naoEncontrado("Provedor não encontrado."));
        var chave = alteracao.novaChave().map(c -> new ChaveCifrada(cofre.cifrar(c), ChaveCifrada.finalDe(c)));
        configuracao.ajustar(alteracao.ajuste(), chave, autor);
        return ProvedorConfigurado.de(configuracoes.save(configuracao), executor.gastoDoMes(provedor));
    }

    /**
     * Testa com a chave e o modelo informados ou, se vazios, com os já guardados. Não envia dado de cliente e não
     * grava nada.
     */
    @Transactional(readOnly = true)
    public ResultadoDoTeste testar(ProvedorDeIa provedor, Optional<String> chaveInformada,
                                   Optional<String> modeloInformado) {
        var configuracao = configuracoes.findById(provedor)
                .orElseThrow(() -> NegocioException.naoEncontrado("Provedor não encontrado."));
        var modelo = modeloInformado.filter(m -> !m.isBlank()).or(configuracao::getModelo);
        var chave = chaveInformada.filter(c -> !c.isBlank()).or(() -> configuracao.getChaveCifrada().map(cofre::decifrar));
        if (modelo.isEmpty() || chave.isEmpty())
            return new ResultadoDoTeste(false, "Informe o modelo e a chave para testar.");
        try {
            executor.cliente(provedor).gerar(PEDIDO_DE_TESTE, new AcessoAoProvedor(modelo.get(), chave.get()));
            return new ResultadoDoTeste(true, "Conexão com " + provedor.nome() + " funcionando.");
        } catch (FalhaNoProvedor e) {
            return new ResultadoDoTeste(false, "A conexão falhou: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<RotaConfigurada> rotas() {
        return rotas.findAll().stream()
                .sorted(Comparator.comparing(r -> r.getTarefa().ordinal()))
                .map(RotaConfigurada::de)
                .toList();
    }

    @Transactional
    public RotaConfigurada definirRota(TarefaDeIa tarefa, DefinicaoDaRota definicao, UUID autor) {
        var rota = rotas.buscarParaAlteracao(tarefa)
                .orElseThrow(() -> NegocioException.naoEncontrado("Tarefa não encontrada."));
        rota.definir(definicao.versao(), definicao.principal(), definicao.reserva(), autor);
        return RotaConfigurada.de(rotas.save(rota));
    }

    /** O uso do mês corrente (Brasília) por provedor. */
    @Transactional(readOnly = true)
    public List<UsoPorProvedor> usoDoMes() {
        var inicio = LocalDate.now(relogio).withDayOfMonth(1).atStartOfDay(relogio.getZone());
        return usos.usoNoPeriodo(inicio.toOffsetDateTime(), inicio.plusMonths(1).toOffsetDateTime());
    }
}
