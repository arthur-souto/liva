package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import sus.liva.domain.model.agendamento.TipoLembrete;

import java.util.Optional;
import java.util.UUID;

interface RegistroMensagemJpaRepository extends JpaRepository<RegistroMensagemEntity, Long> {
    boolean existsByAgendamentoIdAndTipoAndSucessoTrue(UUID agendamentoId, TipoLembrete tipo);
    Optional<RegistroMensagemEntity> findFirstByChaveDestinoAndTipoAndSucessoTrueOrderByEnviadoEmDescIdDesc(String chaveDestino, TipoLembrete tipo);
}
