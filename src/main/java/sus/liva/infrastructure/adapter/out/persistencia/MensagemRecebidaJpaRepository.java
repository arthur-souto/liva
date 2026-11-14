package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface MensagemRecebidaJpaRepository extends JpaRepository<MensagemRecebidaEntity, Long> {
    List<MensagemRecebidaEntity> findByAgendamentoIdOrderByRecebidaEmAscIdAsc(UUID agendamentoId);
}
