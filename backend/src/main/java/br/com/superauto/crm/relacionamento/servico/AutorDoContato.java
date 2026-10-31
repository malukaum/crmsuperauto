package br.com.superauto.crm.relacionamento.servico;

import java.util.Optional;
import java.util.UUID;

/** Dados do caso de uso de relacionamento. */
public record AutorDoContato(UUID id, Optional<UUID> vendedor) {}
