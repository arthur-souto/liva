package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PacienteJpaRepository extends JpaRepository<PacienteEntity, UUID> {
    Optional<PacienteEntity> findByCpf(String cpf);
    List<PacienteEntity> findByTelefone(String telefone);
}
