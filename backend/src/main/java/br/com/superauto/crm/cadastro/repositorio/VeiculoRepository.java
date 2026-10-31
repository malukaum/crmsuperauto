package br.com.superauto.crm.cadastro.repositorio;

import java.util.Optional;

import br.com.superauto.crm.cadastro.entidade.Placa;
import br.com.superauto.crm.cadastro.entidade.Veiculo;

import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório do agregado {@link Veiculo}. */
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    Optional<Veiculo> findByPlaca(Placa placa);
}
