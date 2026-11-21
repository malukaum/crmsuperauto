package br.com.superauto.crm.cadastro.repositorio;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import br.com.superauto.crm.cadastro.entidade.Veiculo;
import br.com.superauto.crm.compartilhado.entidade.Placa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositório do agregado {@link Veiculo}. */
public interface VeiculoRepository extends JpaRepository<Veiculo, Long> {

    Optional<Veiculo> findByPlaca(Placa placa);

    /** RG-06 · O veículo da placa, travado até o fim da transação, para reaproveitá-lo num novo contrato. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Veiculo v where v.placa = :placa")
    Optional<Veiculo> buscarPelaPlacaParaAlteracao(Placa placa);
}
