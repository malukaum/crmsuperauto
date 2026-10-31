package br.com.superauto.crm.cadastro.repositorio;

import br.com.superauto.crm.cadastro.entidade.Veiculo;
import br.com.superauto.crm.compartilhado.entidade.Placa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório do agregado {@link Veiculo}. */
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    Optional<Veiculo> findByPlaca(Placa placa);
}
