package br.com.superauto.crm.cadastro.api;

import br.com.superauto.crm.cadastro.entidade.Cadastrante;
import br.com.superauto.crm.cadastro.entidade.DadosPessoais;
import br.com.superauto.crm.cadastro.servico.CadastroContratoService;
import br.com.superauto.crm.cadastro.servico.CondicoesContrato;
import br.com.superauto.crm.cadastro.servico.ContratoRegistrado;
import br.com.superauto.crm.cadastro.servico.NovaPessoa;
import br.com.superauto.crm.cadastro.servico.NovoCadastro;
import br.com.superauto.crm.cadastro.servico.NovoVeiculo;
import br.com.superauto.crm.compartilhado.entidade.Documento;
import br.com.superauto.crm.compartilhado.entidade.Email;
import br.com.superauto.crm.compartilhado.entidade.Mensalidade;
import br.com.superauto.crm.compartilhado.entidade.Placa;
import br.com.superauto.crm.compartilhado.entidade.Telefone;
import br.com.superauto.crm.seguranca.Papeis;
import br.com.superauto.crm.seguranca.UsuarioAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CAR-02 · Cadastrar cliente, veículo e contrato. Vendedor e gerencial. */
@RestController
@RequestMapping("/api/interno/contratos")
@PreAuthorize(Papeis.GERENCIAL_OU_VENDEDOR)
public class ContratosController {

    private final CadastroContratoService cadastro;

    public ContratosController(CadastroContratoService cadastro) {
        this.cadastro = cadastro;
    }

    /** {@code pessoa} só é usada (e exigida) quando o documento ainda não está na carteira. */
    public record NovoContratoRequest(
            @NotBlank(message = "Informe o CPF ou CNPJ.") String documento,
            @Valid PessoaRequest pessoa,
            @NotNull(message = "Informe o veículo.") @Valid VeiculoRequest veiculo,
            @NotNull(message = "Informe os dados do contrato.") @Valid CondicoesRequest contrato) {

        NovoCadastro comando() {
            return new NovoCadastro(Documento.de(documento), Optional.ofNullable(pessoa).map(PessoaRequest::novaPessoa),
                    veiculo.novoVeiculo(), contrato.condicoes());
        }
    }

    public record PessoaRequest(
            @NotBlank(message = "Informe o nome.") @Size(max = 200, message = "O nome pode ter até 200 caracteres.")
            String nome,
            @NotBlank(message = "Informe o WhatsApp.") String whatsapp,
            @jakarta.validation.constraints.Email(message = "Informe um e-mail válido.") @Size(max = 254, message = "O e-mail pode ter até 254 caracteres.")
            String email,
            LocalDate dataNascimento,
            @NotNull(message = "Informe a cidade e a UF.") Integer municipioId) {

        NovaPessoa novaPessoa() {
            return new NovaPessoa(new DadosPessoais(nome, dataNascimento, municipioId), Telefone.de(whatsapp),
                    Email.seValido(email));
        }
    }

    public record VeiculoRequest(
            @NotBlank(message = "Informe a placa.") String placa,
            @NotBlank(message = "Informe o modelo.") @Size(max = 150, message = "O modelo pode ter até 150 caracteres.")
            String modelo,
            @NotNull(message = "Escolha o tipo de veículo.") Short tipoVeiculoId) {

        NovoVeiculo novoVeiculo() {
            return new NovoVeiculo(Placa.de(placa), modelo, tipoVeiculoId);
        }
    }

    public record CondicoesRequest(
            @NotNull(message = "Informe a mensalidade.")
            @DecimalMin(value = "0.00", message = "A mensalidade não pode ser negativa.")
            @Digits(integer = 10, fraction = 2, message = "A mensalidade aceita no máximo dois decimais.")
            BigDecimal mensalidade,
            @NotNull(message = "Informe a data de adesão (cadastro no SGA).") LocalDate dataAdesao,
            LocalDate dataVenda,
            @Size(max = 30, message = "O código do SGA pode ter até 30 caracteres.") String codigoSga,
            UUID vendedorId) {

        CondicoesContrato condicoes() {
            return new CondicoesContrato(new Mensalidade(mensalidade), dataAdesao, Optional.ofNullable(dataVenda),
                    Optional.ofNullable(codigoSga), Optional.ofNullable(vendedorId));
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContratoRegistrado cadastrar(@Valid @RequestBody NovoContratoRequest req, JwtAuthenticationToken auth) {
        var cadastrante = new Cadastrante(UsuarioAtual.exigirId(auth), UsuarioAtual.temPapel(auth, Papeis.GERENCIAL));
        return cadastro.cadastrar(req.comando(), cadastrante);
    }
}
