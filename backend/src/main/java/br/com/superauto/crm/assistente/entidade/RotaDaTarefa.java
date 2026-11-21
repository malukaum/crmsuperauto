package br.com.superauto.crm.assistente.entidade;

import br.com.superauto.crm.erro.NegocioException;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.springframework.data.domain.AbstractAggregateRoot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** IA-05 · Raiz do agregado: qual provedor atende uma tarefa do assistente (principal) e qual assume se ele falhar. */
@Entity
@Table(name = "ia_tarefa")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RotaDaTarefa extends AbstractAggregateRoot<RotaDaTarefa> {

    @Getter
    @Id
    @Enumerated(EnumType.STRING)
    private TarefaDeIa tarefa;

    @Getter
    @Enumerated(EnumType.STRING)
    private ProvedorDeIa principal;

    @Enumerated(EnumType.STRING)
    private ProvedorDeIa reserva;

    @Getter
    @Version
    private int versao;

    public void definir(int versaoVista, ProvedorDeIa novoPrincipal, Optional<ProvedorDeIa> novaReserva, UUID autor) {
        if (versao != versaoVista) throw NegocioException.edicaoSimultanea();
        Objects.requireNonNull(novoPrincipal, "principal");
        if (novaReserva.filter(novoPrincipal::equals).isPresent())
            throw NegocioException.invalido("reserva-igual", "O provedor reserva precisa ser diferente do principal.");
        var anterior = situacao();
        principal = novoPrincipal;
        reserva = novaReserva.orElse(null);
        registerEvent(new ConfiguracaoDeIaAlterada("ia_tarefa", tarefa.name(), anterior, situacao(), autor));
    }

    /** A ordem de tentativa: primeiro o principal, depois o reserva. */
    public List<ProvedorDeIa> ordemDeTentativa() {
        List<ProvedorDeIa> ordem = new ArrayList<>(List.of(principal));
        getReserva().ifPresent(ordem::add);
        return List.copyOf(ordem);
    }

    public Optional<ProvedorDeIa> getReserva() {
        return Optional.ofNullable(reserva);
    }

    private Map<String, Object> situacao() {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("principal", principal == null ? null : principal.name());
        dados.put("reserva", reserva == null ? null : reserva.name());
        return dados;
    }
}
