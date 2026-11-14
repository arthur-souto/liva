package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sus.liva.domain.model.triagem.StatusTriagem;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TriagemJpaRepository extends JpaRepository<TriagemEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TriagemEntity t where t.id = :id")
    Optional<TriagemEntity> buscarComTrava(@Param("id") UUID id);

    List<TriagemEntity> findByPacienteIdOrderByChegadaEmDesc(UUID pacienteId);

    List<TriagemEntity> findByStatusInOrderByChegadaEm(Collection<StatusTriagem> status);

    boolean existsByPacienteIdAndStatusIn(UUID pacienteId, Collection<StatusTriagem> status);
}
