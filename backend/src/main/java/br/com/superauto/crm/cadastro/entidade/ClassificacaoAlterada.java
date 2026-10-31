package br.com.superauto.crm.cadastro.entidade;

import java.util.UUID;

/** Evento de domínio: perfil ou potencial de indicação do associado mudaram (CAR-06). */
public record ClassificacaoAlterada(Long associadoId, Classificacao anterior, Classificacao nova, UUID autor) {
}
