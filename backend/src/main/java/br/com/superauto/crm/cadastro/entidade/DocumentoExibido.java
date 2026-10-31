package br.com.superauto.crm.cadastro.entidade;

import java.util.UUID;

/** Evento de domínio: alguém viu o CPF/CNPJ completo do associado ao clicar em "mostrar" (RG-04, proposta). */
public record DocumentoExibido(Long associadoId, UUID autor) {
}
