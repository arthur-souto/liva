package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface EvolucaoClinicaJpaRepository extends JpaRepository<EvolucaoClinicaEntity, Long> {
    List<EvolucaoClinicaEntity> findByAgendamentoIdInOrderByRegistradaEm(Collection<UUID> agendamentoIds);
}
