package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import sus.liva.domain.model.profissional.TipoProfissional;

import java.util.List;
import java.util.UUID;

interface ProfissionalJpaRepository extends JpaRepository<ProfissionalEntity, UUID> {
    List<ProfissionalEntity> findByTipoAndEspecialidadeAndAtivoTrueOrderByNome(TipoProfissional tipo, String especialidade);
}
