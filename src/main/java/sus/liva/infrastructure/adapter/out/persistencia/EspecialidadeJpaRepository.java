package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface EspecialidadeJpaRepository extends JpaRepository<EspecialidadeEntity, String> {
    List<EspecialidadeEntity> findAll(Sort sort);
}
