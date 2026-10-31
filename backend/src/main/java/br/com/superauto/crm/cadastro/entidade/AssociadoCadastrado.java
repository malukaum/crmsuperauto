package br.com.superauto.crm.cadastro.entidade;

import java.util.List;
import java.util.UUID;

/** Evento de domínio: uma pessoa entrou na carteira (CAR-02). */
public record AssociadoCadastrado(Long associadoId, String nome, String documento, Integer municipioId,
                                  List<ContatoResumo> contatos, UUID autor) {
}
