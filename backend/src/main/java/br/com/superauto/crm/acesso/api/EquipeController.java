package br.com.superauto.crm.acesso.api;

import java.util.List;

import br.com.superauto.crm.acesso.servico.ConsultaEquipeService;
import br.com.superauto.crm.acesso.servico.VendedorDaEquipe;
import br.com.superauto.crm.seguranca.Papeis;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Vendedores ativos, para o filtro de vendedor da carteira (CAR-01) e a escolha do vendedor do contrato (CAR-02,
 * CAR-05). O vendedor não usa: o filtro não aparece para ele (RG-03).
 */
@RestController
@RequestMapping("/api/interno/equipe")
public class EquipeController {

    private final ConsultaEquipeService consulta;

    public EquipeController(ConsultaEquipeService consulta) {
        this.consulta = consulta;
    }

    @GetMapping("/vendedores")
    @PreAuthorize(Papeis.GERENCIAL_OU_ATENDENTE)
    public List<VendedorDaEquipe> vendedores() {
        return consulta.vendedoresAtivos();
    }
}
