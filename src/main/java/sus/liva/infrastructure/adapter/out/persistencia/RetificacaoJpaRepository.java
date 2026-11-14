package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface RetificacaoJpaRepository extends JpaRepository<RetificacaoEntity, Long> {
    List<RetificacaoEntity> findByTriagemIdOrderByRegistradaEm(UUID triagemId);
}
