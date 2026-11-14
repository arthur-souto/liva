package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PacienteJpaRepository extends JpaRepository<PacienteEntity, UUID> {
}
