package br.com.superauto.crm.relacionamento.servico;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import br.com.superauto.crm.relacionamento.entidade.HistoricoExportado;
import br.com.superauto.crm.relacionamento.entidade.PeriodoDoHistorico;
import br.com.superauto.crm.relacionamento.repositorio.AutorDeAtendimento;
import br.com.superauto.crm.relacionamento.repositorio.ConsultaHistorico;
import br.com.superauto.crm.relacionamento.repositorio.FiltroDoHistorico;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * ATD-05 · Consultar e exportar o histórico de atendimentos, do mais recente para o mais antigo, no escopo do papel
 * (RG-03). Cada exportação vai para a auditoria com o filtro e a quantidade de linhas (CAR-07).
 */
@Service
@RequiredArgsConstructor
public class ConsultaHistoricoService {
    private final ConsultaHistorico historico;
    private final ApplicationEventPublisher eventos;
    private final Clock relogio;

    @Transactional(readOnly = true)
    public Page<LinhaDoHistorico> listar(ConsultaDoHistorico consulta, Optional<UUID> vendedor, Pageable pagina) {
        return historico.listar(pedido(consulta, vendedor), pagina)
                .map(linha -> LinhaDoHistorico.de(linha, relogio.getZone()));
    }

    @Transactional(readOnly = true)
    public List<AutorDeAtendimento> autores(Optional<UUID> vendedor) {
        return historico.autores(vendedor);
    }

    @Transactional
    public List<LinhaDoHistorico> exportar(ConsultaDoHistorico consulta, AutorDoContato autor) {
        var linhas = historico.todos(pedido(consulta, autor.vendedor())).stream()
                .map(linha -> LinhaDoHistorico.de(linha, relogio.getZone()))
                .toList();
        eventos.publishEvent(new HistoricoExportado(
                informados(consulta.periodo(), consulta.filtro()), linhas.size(), autor.id()));
        return linhas;
    }

    private ConsultaHistorico.Pedido pedido(ConsultaDoHistorico consulta, Optional<UUID> vendedor) {
        var fuso = relogio.getZone();
        return new ConsultaHistorico.Pedido(
                consulta.periodo().inicioNoFuso(fuso), consulta.periodo().fimNoFuso(fuso), consulta.filtro(), vendedor);
    }

    private static Map<String, Object> informados(PeriodoDoHistorico periodo, FiltroDoHistorico filtro) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("inicio", periodo.inicio().toString());
        dados.put("fim", periodo.fim().toString());
        filtro.tipo().ifPresent(tipo -> dados.put("tipo", tipo.name()));
        filtro.resultado().ifPresent(resultado -> dados.put("resultado", resultado.name()));
        filtro.usuario().ifPresent(usuario -> dados.put("usuario", usuario.toString()));
        filtro.cidade().ifPresent(cidade -> dados.put("cidade", cidade));
        filtro.busca().ifPresent(busca -> dados.put("busca", busca));
        return dados;
    }
}
