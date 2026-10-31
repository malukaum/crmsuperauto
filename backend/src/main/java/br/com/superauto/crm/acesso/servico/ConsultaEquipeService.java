package br.com.superauto.crm.acesso.servico;

import java.text.Collator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import br.com.superauto.crm.acesso.entidade.Conta;
import br.com.superauto.crm.acesso.repositorio.ContratosDoVendedor;
import br.com.superauto.crm.acesso.repositorio.ProvedorIdentidade;
import br.com.superauto.crm.seguranca.Papeis;

import org.springframework.stereotype.Service;

/** Lado de leitura da gestão de usuários (ACE-04): quem está na equipe, com papel e carteira de cada um. */
@Service
public class ConsultaEquipeService {

    private static final Locale PORTUGUES_DO_BRASIL = Locale.of("pt", "BR");

    private final ProvedorIdentidade identidade;
    private final ContratosDoVendedor contratos;

    public ConsultaEquipeService(ProvedorIdentidade identidade, ContratosDoVendedor contratos) {
        this.identidade = identidade;
        this.contratos = contratos;
    }

    /** Todos com papel da equipe, ativos primeiro e em ordem alfabética. Clientes e contas técnicas ficam de fora. */
    public List<UsuarioEquipe> listar() {
        Map<UUID, Conta> porId = new LinkedHashMap<>();
        Map<UUID, String> papelPorId = new LinkedHashMap<>();
        // Papeis.EQUIPE está do mais amplo para o mais restrito: quem tiver dois fica com o mais amplo.
        for (String papel : Papeis.EQUIPE) {
            for (Conta conta : identidade.contasComPapel(papel)) {
                porId.putIfAbsent(conta.id(), conta);
                papelPorId.putIfAbsent(conta.id(), papel);
            }
        }
        Map<UUID, Long> carteiras = contratos.contarPorVendedor();
        return porId.values().stream()
                .filter(conta -> !RegrasEquipe.ehContaTecnica(conta))
                .sorted(Comparator.comparing(Conta::ativo).reversed()
                        .thenComparing(Conta::nomeCompleto, Collator.getInstance(PORTUGUES_DO_BRASIL)))
                .map(conta -> UsuarioEquipe.de(conta, papelPorId.get(conta.id()), carteiras.getOrDefault(conta.id(), 0L)))
                .toList();
    }
}
