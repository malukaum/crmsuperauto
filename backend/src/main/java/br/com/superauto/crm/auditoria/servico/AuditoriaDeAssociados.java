package br.com.superauto.crm.auditoria.servico;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import br.com.superauto.crm.auditoria.entidade.Alteracao;
import br.com.superauto.crm.auditoria.entidade.EventoAuditoria;
import br.com.superauto.crm.auditoria.entidade.ItemAuditado;
import br.com.superauto.crm.cadastro.entidade.AssociadoAnonimizado;
import br.com.superauto.crm.cadastro.entidade.AssociadoCadastrado;
import br.com.superauto.crm.cadastro.entidade.ClassificacaoAlterada;
import br.com.superauto.crm.cadastro.entidade.ContatoResumo;
import br.com.superauto.crm.cadastro.entidade.ContatosAlterados;
import br.com.superauto.crm.cadastro.entidade.DadosPessoais;
import br.com.superauto.crm.cadastro.entidade.DadosPessoaisCorrigidos;
import br.com.superauto.crm.cadastro.entidade.DocumentoExibido;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Registra na trilha de auditoria (RG-08) os eventos de domínio do contexto Cadastro sobre associados: cadastro,
 * dados pessoais, contatos, classificação, exibição do documento (RG-04) e anonimização (CAR-08). Roda antes do
 * commit, na mesma transação da alteração.
 */
@Component
public class AuditoriaDeAssociados {

    static final String ENTIDADE = "associado";
    private static final String CAMPO_CONTATOS = "contatos";

    private final RegistroAuditoria auditoria;

    public AuditoriaDeAssociados(RegistroAuditoria auditoria) {
        this.auditoria = auditoria;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoCadastrar(AssociadoCadastrado evento) {
        Map<String, Object> novo = new LinkedHashMap<>();
        novo.put("nome", evento.nome());
        novo.put("documento", evento.documento());
        novo.put("municipioId", evento.municipioId());
        novo.put(CAMPO_CONTATOS, contatos(evento.contatos()));
        auditoria.registrar(EventoAuditoria.criacao(item(evento.associadoId()), evento.autor(), novo));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoCorrigirDados(DadosPessoaisCorrigidos evento) {
        Map<String, Object> anterior = new LinkedHashMap<>();
        Map<String, Object> novo = new LinkedHashMap<>();
        comparar("nome", evento.anteriores().nome(), evento.novos().nome(), anterior, novo);
        comparar("dataNascimento", texto(evento.anteriores()), texto(evento.novos()), anterior, novo);
        comparar("municipioId", evento.anteriores().municipioId(), evento.novos().municipioId(), anterior, novo);
        alterou(evento.associadoId(), evento.autor(), new Alteracao(anterior, novo));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoAlterarContatos(ContatosAlterados evento) {
        alterou(evento.associadoId(), evento.autor(), new Alteracao(
                Map.of(CAMPO_CONTATOS, contatos(evento.anteriores())), Map.of(CAMPO_CONTATOS, contatos(evento.atuais()))));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoClassificar(ClassificacaoAlterada evento) {
        Map<String, Object> anterior = new LinkedHashMap<>();
        Map<String, Object> novo = new LinkedHashMap<>();
        comparar("perfil", evento.anterior().perfil(), evento.nova().perfil(), anterior, novo);
        comparar("potencialIndicacao", evento.anterior().potencial(), evento.nova().potencial(), anterior, novo);
        alterou(evento.associadoId(), evento.autor(), new Alteracao(anterior, novo));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoExibirDocumento(DocumentoExibido evento) {
        auditoria.registrar(EventoAuditoria.visualizacao(item(evento.associadoId()), evento.autor(),
                Map.of("campo", "documento")));
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void aoAnonimizar(AssociadoAnonimizado evento) {
        auditoria.registrar(EventoAuditoria.anonimizacao(item(evento.associadoId()), evento.autor()));
    }

    private void alterou(Long associadoId, UUID autor, Alteracao alteracao) {
        auditoria.registrar(EventoAuditoria.alteracao(item(associadoId), autor, alteracao));
    }

    /** Só os campos que mudaram entram no registro. */
    private static void comparar(String campo, Object antes, Object depois, Map<String, Object> anterior,
                                 Map<String, Object> novo) {
        if (!Objects.equals(antes, depois)) {
            anterior.put(campo, antes == null ? null : String.valueOf(antes));
            novo.put(campo, depois == null ? null : String.valueOf(depois));
        }
    }

    private static String texto(DadosPessoais dados) {
        return dados.dataNascimento() == null ? null : dados.dataNascimento().toString();
    }

    private static List<Map<String, Object>> contatos(List<ContatoResumo> contatos) {
        return contatos.stream().map(c -> Map.<String, Object>of("tipo", c.tipo().name(), "valor", c.valor(),
                "principal", c.principal(), "aceitaContato", c.aceitaContato())).toList();
    }

    private static ItemAuditado item(Long associadoId) {
        return ItemAuditado.de(ENTIDADE, associadoId);
    }
}
