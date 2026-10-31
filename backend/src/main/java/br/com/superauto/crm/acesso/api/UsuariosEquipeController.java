package br.com.superauto.crm.acesso.api;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import br.com.superauto.crm.acesso.servico.ConsultaEquipeService;
import br.com.superauto.crm.acesso.servico.GestaoUsuariosService;
import br.com.superauto.crm.acesso.servico.NovoUsuario;
import br.com.superauto.crm.acesso.servico.ResultadoCriacaoUsuario;
import br.com.superauto.crm.acesso.servico.UsuarioEquipe;
import br.com.superauto.crm.erro.NegocioException;
import br.com.superauto.crm.seguranca.Papeis;
import br.com.superauto.crm.seguranca.UsuarioAtual;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ACE-04 · Gerenciar usuários da equipe. Só o gerencial. */
@RestController
@RequestMapping("/api/gestao/usuarios")
@PreAuthorize(Papeis.SO_GERENCIAL)
public class UsuariosEquipeController {

    private final GestaoUsuariosService servico;
    private final ConsultaEquipeService consulta;

    public UsuariosEquipeController(GestaoUsuariosService servico, ConsultaEquipeService consulta) {
        this.servico = servico;
        this.consulta = consulta;
    }

    public record NovoUsuarioRequest(
            @NotBlank(message = "Informe o usuário.")
            @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$",
                    message = "O usuário deve ter de 3 a 50 caracteres: letras sem acento, números, ponto, hífen ou sublinhado.")
            String username,
            @NotBlank(message = "Informe o nome.") @Size(max = 100, message = "O nome pode ter até 100 caracteres.")
            String nome,
            @NotBlank(message = "Informe o sobrenome.") @Size(max = 100, message = "O sobrenome pode ter até 100 caracteres.")
            String sobrenome,
            @NotBlank(message = "Informe o e-mail.") @Email(message = "Informe um e-mail válido.")
            @Size(max = br.com.superauto.crm.compartilhado.entidade.Email.TAMANHO_MAXIMO,
                    message = "O e-mail pode ter até 254 caracteres.")
            String email,
            @NotBlank(message = "Escolha o papel.")
            String papel) {
    }

    public record AlterarPapelRequest(@NotBlank(message = "Escolha o papel.") String papel) {
    }

    public record DesativarRequest(UUID transferirParaId) {
    }

    @GetMapping
    public List<UsuarioEquipe> listar() {
        return consulta.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResultadoCriacaoUsuario criar(@Valid @RequestBody NovoUsuarioRequest req, JwtAuthenticationToken auth) {
        return servico.criar(new NovoUsuario(req.username(), req.nome(), req.sobrenome(), req.email(), req.papel()),
                autor(auth));
    }

    @PutMapping("/{id}/papel")
    public UsuarioEquipe alterarPapel(@PathVariable UUID id, @Valid @RequestBody AlterarPapelRequest req,
                                      JwtAuthenticationToken auth) {
        return servico.alterarPapel(id, req.papel(), autor(auth));
    }

    @PostMapping("/{id}/desativar")
    public UsuarioEquipe desativar(@PathVariable UUID id, @RequestBody(required = false) DesativarRequest req,
                                   JwtAuthenticationToken auth) {
        return servico.desativar(id, req == null ? null : req.transferirParaId(), autor(auth));
    }

    @PostMapping("/{id}/reativar")
    public UsuarioEquipe reativar(@PathVariable UUID id, JwtAuthenticationToken auth) {
        return servico.reativar(id, autor(auth));
    }

    @PostMapping("/{id}/convite")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviarConvite(@PathVariable UUID id) {
        servico.reenviarConvite(id);
    }

    private static UUID autor(JwtAuthenticationToken auth) {
        return UsuarioAtual.id(auth.getToken())
                .orElseThrow(() -> NegocioException.invalido("token-sem-id", "O token não identifica o usuário."));
    }
}
