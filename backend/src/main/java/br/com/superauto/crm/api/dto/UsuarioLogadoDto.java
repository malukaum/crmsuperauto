package br.com.superauto.crm.api.dto;

import java.util.List;

/**
 * Dados do usuário autenticado, extraídos do token.
 *
 * @param cpf preenchido só para usuários com o atributo {@code cpf} no Keycloak (área do cliente)
 */
public record UsuarioLogadoDto(String username, String nome, String email, List<String> papeis, String cpf) {
}
