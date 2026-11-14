package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface DisponibilidadeJpaRepository extends JpaRepository<DisponibilidadeEntity, UUID> {
    List<DisponibilidadeEntity> findByProfissionalIdOrderByDiaSemanaAscHoraInicioAsc(UUID profissionalId);
}
