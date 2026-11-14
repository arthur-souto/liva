package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AgendamentoJpaRepository extends JpaRepository<AgendamentoEntity, UUID> {
    List<AgendamentoEntity> findByDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHora(LocalDateTime inicio, LocalDateTime fim);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AgendamentoEntity a where a.id = :id")
    Optional<AgendamentoEntity> buscarComTrava(@Param("id") UUID id);

    Optional<AgendamentoEntity> findByChaveIdempotencia(String chaveIdempotencia);

    List<AgendamentoEntity> findByEncaminhamentoIdOrderByDataHora(UUID encaminhamentoId);

    List<AgendamentoEntity> findByPacienteIdOrderByDataHora(UUID pacienteId);

    List<AgendamentoEntity> findByProfissionalIdAndDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHora(
            UUID profissionalId, LocalDateTime inicio, LocalDateTime fim);
}
