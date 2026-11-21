package br.com.superauto.crm.assistente.servico;

import br.com.superauto.crm.assistente.entidade.TextoPseudonimizado;
import br.com.superauto.crm.assistente.entidade.DadosIdentificaveis;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IA-06 · Serviço de domínio: troca o que identifica pessoas por marcadores ([CLIENTE], [PLACA_1]...) antes de o
 * texto ir para o provedor de IA, e restaura a resposta depois, no nosso servidor.
 *
 * <p>O e-mail sai primeiro, para o nome dentro do endereço não o desmontar; depois o nome do cliente e, por padrão,
 * qualquer CPF, CNPJ, placa ou telefone que apareça no texto, de quem quer que seja.
 */
public final class Pseudonimizacao {
    private static final String CLIENTE = "[CLIENTE]";
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern CPF_OU_CNPJ =
            Pattern.compile("\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b|\\b\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}\\b");
    private static final Pattern PLACA = Pattern.compile("\\b[A-Za-z]{3}-?\\d[A-Za-z0-9]\\d{2}\\b");
    private static final Pattern TELEFONE =
            Pattern.compile("(\\+?55\\s?)?\\(?0?\\d{2}\\)?\\s?9?\\s?\\d{4}[\\s.-]?\\d{4}\\b");
    private static final int MINIMO_PARA_NOME = 3;

    public TextoPseudonimizado pseudonimizar(String texto, DadosIdentificaveis cliente) {
        var marcadores = new LinkedHashMap<String, String>();
        String resultado = texto == null ? "" : texto;
        resultado = trocarPadrao(resultado, EMAIL, "EMAIL", marcadores);
        resultado = trocarNome(resultado, cliente.nome(), marcadores);
        resultado = trocarPadrao(resultado, CPF_OU_CNPJ, "DOCUMENTO", marcadores);
        resultado = trocarPadrao(resultado, PLACA, "PLACA", marcadores);
        resultado = trocarPadrao(resultado, TELEFONE, "TELEFONE", marcadores);
        return new TextoPseudonimizado(resultado, marcadores);
    }

    /** Devolve os valores originais no lugar dos marcadores, na resposta do provedor. */
    public String restaurar(String texto, Map<String, String> marcadores) {
        if (texto == null) return null;
        String resultado = texto;
        for (var marcador : marcadores.entrySet()) resultado = resultado.replace(marcador.getKey(), marcador.getValue());
        return resultado;
    }

    /** Nome completo e cada parte do nome com 3 letras ou mais (José, Silva), sem diferença de acento ou caixa. */
    private static String trocarNome(String texto, String nome, Map<String, String> marcadores) {
        if (nome == null || nome.isBlank()) return texto;
        List<String> partes = new ArrayList<>();
        partes.add(nome.strip());
        for (String parte : nome.strip().split("\\s+")) if (parte.length() >= MINIMO_PARA_NOME) partes.add(parte);
        String resultado = texto;
        boolean trocou = false;
        for (String parte : partes) {
            Matcher m = Pattern.compile("(?iu)\\b" + padraoSemAcento(parte) + "\\b").matcher(resultado);
            if (m.find()) {
                resultado = m.replaceAll(Matcher.quoteReplacement(CLIENTE));
                trocou = true;
            }
        }
        if (trocou) marcadores.put(CLIENTE, nome.strip());
        return resultado;
    }

    private static String trocarPadrao(String texto, Pattern padrao, String tipo, Map<String, String> marcadores) {
        Matcher m = padrao.matcher(texto);
        StringBuilder saida = new StringBuilder();
        Map<String, String> porValor = new LinkedHashMap<>();
        while (m.find()) {
            String valor = m.group();
            String marcador = porValor.computeIfAbsent(valor,
                    v -> "[" + tipo + "_" + (porValor.size() + 1) + "]");
            marcadores.put(marcador, valor);
            m.appendReplacement(saida, Matcher.quoteReplacement(marcador));
        }
        m.appendTail(saida);
        return saida.toString();
    }

    /** Monta um padrão que aceita a palavra com ou sem acento (José ↔ Jose). */
    private static String padraoSemAcento(String palavra) {
        String base = Normalizer.normalize(palavra, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        StringBuilder padrao = new StringBuilder();
        for (char c : base.toCharArray()) {
            padrao.append(switch (Character.toLowerCase(c)) {
                case 'a' -> "[aáàâã]";
                case 'e' -> "[eéê]";
                case 'i' -> "[ií]";
                case 'o' -> "[oóôõ]";
                case 'u' -> "[uúü]";
                case 'c' -> "[cç]";
                default -> Pattern.quote(String.valueOf(c));
            });
        }
        return padrao.toString();
    }
}
