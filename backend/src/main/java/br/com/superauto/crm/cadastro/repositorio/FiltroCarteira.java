package br.com.superauto.crm.cadastro.repositorio;

import br.com.superauto.crm.cadastro.entidade.Perfil;
import br.com.superauto.crm.cadastro.entidade.PotencialIndicacao;
import br.com.superauto.crm.cadastro.entidade.SituacaoContrato;
import br.com.superauto.crm.compartilhado.entidade.Fila;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Filtros da carteira (CAR-01). Tudo opcional; {@code vendedorId} preenchido restringe aos contratos desse
 * vendedor, seja pelo filtro do gerencial, seja pelo escopo do próprio vendedor (RG-03).
 */
public record FiltroCarteira(Optional<TermoBusca> busca, Optional<Fila> fila, Optional<Integer> municipioId,
                             Optional<SituacaoContrato> situacao, Optional<Short> etapaId,
                             Optional<Short> tipoVeiculoId, Optional<Perfil> perfil,
                             Optional<PotencialIndicacao> potencial, Optional<UUID> vendedorId, boolean excluirCancelados) {

    public FiltroCarteira(Optional<TermoBusca> busca, Optional<Fila> fila, Optional<Integer> municipioId,
                          Optional<SituacaoContrato> situacao, Optional<Short> etapaId, Optional<Short> tipoVeiculoId,
                          Optional<Perfil> perfil, Optional<PotencialIndicacao> potencial, Optional<UUID> vendedorId) {
        this(busca, fila, municipioId, situacao, etapaId, tipoVeiculoId, perfil, potencial, vendedorId, false);
    }

    public FiltroCarteira {
        Objects.requireNonNull(busca);
        Objects.requireNonNull(fila);
        Objects.requireNonNull(municipioId);
        Objects.requireNonNull(situacao);
        Objects.requireNonNull(etapaId);
        Objects.requireNonNull(tipoVeiculoId);
        Objects.requireNonNull(perfil);
        Objects.requireNonNull(potencial);
        Objects.requireNonNull(vendedorId);
    }

    public FiltroCarteira doVendedor(UUID vendedor) {
        return new FiltroCarteira(busca, fila, municipioId, situacao, etapaId, tipoVeiculoId, perfil, potencial,
                Optional.of(vendedor), excluirCancelados);
    }

    /** Os filtros usados, só os informados, para registrar na auditoria da exportação (CAR-07). */
    public Map<String, Object> informados() {
        Map<String, Object> usados = new LinkedHashMap<>();
        busca.ifPresent(b -> usados.put("busca", b.texto()));
        fila.ifPresent(f -> usados.put("fila", f.name()));
        municipioId.ifPresent(m -> usados.put("municipioId", m));
        situacao.ifPresent(s -> usados.put("situacao", s.name()));
        etapaId.ifPresent(e -> usados.put("etapaId", e));
        tipoVeiculoId.ifPresent(t -> usados.put("tipoVeiculoId", t));
        perfil.ifPresent(p -> usados.put("perfil", p.name()));
        potencial.ifPresent(p -> usados.put("potencial", p.name()));
        vendedorId.ifPresent(v -> usados.put("vendedorId", v.toString()));
        if (excluirCancelados) usados.put("excluirCancelados", true);
        return usados;
    }
}
