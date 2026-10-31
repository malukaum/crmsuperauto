package br.com.superauto.crm.referencia.api;

import java.util.List;

import br.com.superauto.crm.referencia.servico.ConsultaReferenciasService;
import br.com.superauto.crm.referencia.servico.ConsultaReferenciasService.Cidade;
import br.com.superauto.crm.referencia.servico.ConsultaReferenciasService.Opcao;
import br.com.superauto.crm.seguranca.Papeis;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Listas de apoio para os formulários e filtros da equipe (CAR-01, CAR-02). */
@RestController
@RequestMapping("/api/interno/referencias")
@PreAuthorize(Papeis.EQUIPE_INTERNA)
public class ReferenciasController {

    private final ConsultaReferenciasService consulta;

    public ReferenciasController(ConsultaReferenciasService consulta) {
        this.consulta = consulta;
    }

    @GetMapping("/cidades")
    public List<Cidade> cidades(@RequestParam(name = "nome", required = false) String nome) {
        return consulta.buscarCidades(nome);
    }

    @GetMapping("/tipos-veiculo")
    public List<Opcao> tiposVeiculo() {
        return consulta.tiposVeiculo();
    }

    @GetMapping("/etapas")
    public List<Opcao> etapas() {
        return consulta.etapasDaRegua();
    }
}
