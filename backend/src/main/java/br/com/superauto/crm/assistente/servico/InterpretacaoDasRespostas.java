package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.Assunto;
import br.com.superauto.crm.assistente.entidade.ChanceDeIndicar;
import br.com.superauto.crm.assistente.entidade.Recomendacao;
import br.com.superauto.crm.assistente.entidade.ResumoDoAtendimento;
import br.com.superauto.crm.assistente.entidade.Satisfacao;
import br.com.superauto.crm.erro.NegocioException;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lê a resposta do provedor no formato combinado e devolve os dados do domínio, já com os marcadores trocados de
 * volta pelos valores originais (IA-06). Resposta fora do formato vira {@code resposta-invalida}, e o assistente
 * tenta de novo, no mesmo provedor ou no reserva.
 */
@Component
public class InterpretacaoDasRespostas {
    private static final int MAXIMO_DE_RECOMENDACOES = 3;
    private final JsonMapper json = JsonMapper.builder().build();
    private final Pseudonimizacao pseudonimizacao;

    public InterpretacaoDasRespostas(Pseudonimizacao pseudonimizacao) {
        this.pseudonimizacao = pseudonimizacao;
    }

    ResumoDoAtendimento resumo(String resposta, Map<String, String> marcadores) {
        JsonNode raiz = ler(resposta);
        return new ResumoDoAtendimento(
                restaurar(texto(raiz, "resumoCurto"), marcadores),
                assuntos(raiz.path("assuntos")),
                Satisfacao.valueOf(texto(raiz, "satisfacao").toUpperCase(Locale.ROOT)),
                lista(raiz.path("objecoes"), marcadores),
                lista(raiz.path("interesses"), marcadores),
                raiz.path("riscoDeCancelamento").asBoolean(false),
                ChanceDeIndicar.valueOf(texto(raiz, "chanceDeIndicar").toUpperCase(Locale.ROOT)),
                restaurar(texto(raiz, "proximaAcao"), marcadores));
    }

    List<Recomendacao> recomendacoes(String resposta, Map<String, String> marcadores) {
        List<Recomendacao> recomendacoes = new ArrayList<>();
        for (JsonNode item : ler(resposta).path("recomendacoes")) {
            if (recomendacoes.size() == MAXIMO_DE_RECOMENDACOES) break;
            recomendacoes.add(new Recomendacao(restaurar(texto(item, "abordagem"), marcadores),
                    restaurar(texto(item, "base"), marcadores)));
        }
        if (recomendacoes.isEmpty()) throw invalida("O provedor não devolveu nenhuma recomendação.");
        return List.copyOf(recomendacoes);
    }

    String textoLivre(String resposta) {
        if (resposta == null || resposta.isBlank()) throw invalida("O provedor devolveu uma resposta vazia.");
        return resposta.strip();
    }

    /** Aceita o JSON puro ou dentro de um bloco ```json, que alguns provedores insistem em usar. */
    private JsonNode ler(String resposta) {
        if (resposta == null) throw invalida("O provedor devolveu uma resposta vazia.");
        String limpa = resposta.strip();
        int inicio = limpa.indexOf('{');
        int fim = limpa.lastIndexOf('}');
        if (inicio < 0 || fim <= inicio) throw invalida("A resposta do provedor não veio no formato combinado.");
        try {
            return json.readTree(limpa.substring(inicio, fim + 1));
        } catch (RuntimeException e) {
            throw invalida("A resposta do provedor não é um JSON válido.");
        }
    }

    private static String texto(JsonNode no, String campo) {
        JsonNode valor = no.path(campo);
        if (valor.isMissingNode() || valor.isNull()) throw invalida("Faltou o campo \"" + campo + "\" na resposta.");
        return valor.asString();
    }

    private static Set<Assunto> assuntos(JsonNode lista) {
        Set<Assunto> assuntos = EnumSet.noneOf(Assunto.class);
        for (JsonNode item : lista) {
            try {
                assuntos.add(Assunto.valueOf(item.asString().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                assuntos.add(Assunto.OUTRO);
            }
        }
        return assuntos;
    }

    private List<String> lista(JsonNode lista, Map<String, String> marcadores) {
        List<String> itens = new ArrayList<>();
        for (JsonNode item : lista) itens.add(restaurar(item.asString(), marcadores));
        return itens;
    }

    private String restaurar(String texto, Map<String, String> marcadores) {
        return pseudonimizacao.restaurar(texto, marcadores);
    }

    private static NegocioException invalida(String mensagem) {
        return NegocioException.invalido("resposta-invalida", mensagem);
    }
}
