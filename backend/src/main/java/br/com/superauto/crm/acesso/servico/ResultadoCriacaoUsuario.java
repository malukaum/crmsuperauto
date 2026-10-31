package br.com.superauto.crm.acesso.servico;

/** Resultado da criação: o usuário e se o e-mail para definir a senha saiu. */
public record ResultadoCriacaoUsuario(UsuarioEquipe usuario, boolean emailEnviado) {
}
