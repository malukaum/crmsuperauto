package br.com.superauto.crm.cadastro.entidade;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import br.com.superauto.crm.erro.NegocioException;

/**
 * As regras dos contatos de um associado (CAR-04), aplicadas sobre a lista mapeada na raiz {@link Associado}:
 * <ul>
 *   <li>o mesmo tipo e número (ou e-mail) não se repete no mesmo associado;</li>
 *   <li>há um principal por tipo: marcar outro como principal desmarca o anterior, e o primeiro de um tipo
 *       já nasce principal;</li>
 *   <li>o associado precisa de ao menos um WhatsApp ou telefone (proposta), e quem sai do principal passa a
 *       vez para o mais antigo do mesmo tipo.</li>
 * </ul>
 */
final class ContatosDoAssociado {

    private final List<ContatoAssociado> contatos;

    ContatosDoAssociado(List<ContatoAssociado> contatos) {
        this.contatos = contatos;
    }

    void adicionar(DadosContato dados) {
        exigirInedito(dados.meio(), Optional.empty());
        var novo = new ContatoAssociado(dados);
        contatos.add(novo);
        organizarPrincipal(novo, dados.principal());
    }

    void alterar(Long contatoId, DadosContato dados) {
        ContatoAssociado contato = exigir(contatoId);
        exigirInedito(dados.meio(), Optional.of(contato));
        var tipoAnterior = contato.getMeio().tipo();
        contato.aplicar(dados);
        exigirTelefoneParaContato();
        organizarPrincipal(contato, dados.principal());
        promoverSeFaltarPrincipal(tipoAnterior);
    }

    void remover(Long contatoId) {
        ContatoAssociado contato = exigir(contatoId);
        contatos.remove(contato);
        exigirTelefoneParaContato();
        promoverSeFaltarPrincipal(contato.getMeio().tipo());
    }

    void apagarTodos() {
        contatos.clear();
    }

    List<ContatoResumo> resumo() {
        return contatos.stream().map(ContatoAssociado::resumo).toList();
    }

    private void organizarPrincipal(ContatoAssociado contato, boolean pediuPrincipal) {
        var tipo = contato.getMeio().tipo();
        if (pediuPrincipal || doTipo(tipo).size() == 1) {
            doTipo(tipo).forEach(ContatoAssociado::deixarDeSerPrincipal);
            contato.tornarPrincipal();
        }
        promoverSeFaltarPrincipal(tipo);
    }

    private void promoverSeFaltarPrincipal(TipoContatoAssociado tipo) {
        var doTipo = doTipo(tipo);
        if (!doTipo.isEmpty() && doTipo.stream().noneMatch(ContatoAssociado::isPrincipal)) {
            doTipo.stream().min(Comparator.comparing(ContatoAssociado::getId,
                    Comparator.nullsLast(Comparator.naturalOrder()))).ifPresent(ContatoAssociado::tornarPrincipal);
        }
    }

    private void exigirTelefoneParaContato() {
        if (contatos.stream().noneMatch(c -> c.getMeio().tipo().telefonico())) {
            throw NegocioException.invalido("contato-telefonico-obrigatorio",
                    "O associado precisa ter ao menos um WhatsApp ou telefone.");
        }
    }

    private void exigirInedito(MeioContato meio, Optional<ContatoAssociado> alterado) {
        boolean repetido = contatos.stream()
                .filter(c -> alterado.map(a -> a != c).orElse(true))
                .anyMatch(c -> c.mesmoMeio(meio));
        if (repetido) {
            throw NegocioException.conflito("contato-repetido", "Este contato já está cadastrado para o associado.");
        }
    }

    private ContatoAssociado exigir(Long contatoId) {
        return contatos.stream().filter(c -> c.getId() != null && c.getId().equals(contatoId)).findFirst()
                .orElseThrow(() -> NegocioException.naoEncontrado("Contato não encontrado."));
    }

    private List<ContatoAssociado> doTipo(TipoContatoAssociado tipo) {
        return contatos.stream().filter(c -> c.doTipo(tipo)).toList();
    }
}
