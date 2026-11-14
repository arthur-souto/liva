package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sus.liva.domain.model.encaminhamento.StatusEncaminhamento;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface EncaminhamentoJpaRepository extends JpaRepository<EncaminhamentoEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EncaminhamentoEntity e where e.id = :id")
    Optional<EncaminhamentoEntity> buscarComTrava(@Param("id") UUID id);

    List<EncaminhamentoEntity> findByTriagemIdOrderByCriadoEm(UUID triagemId);

    List<EncaminhamentoEntity> findByPacienteIdOrderByCriadoEm(UUID pacienteId);

    List<EncaminhamentoEntity> findByEspecialidadeAndStatusInOrderByCriadoEm(String especialidade, Collection<StatusEncaminhamento> status);
}
