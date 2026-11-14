package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sus.liva.domain.model.agendamento.TipoLembrete;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface RegistroMensagemJpaRepository extends JpaRepository<RegistroMensagemEntity, Long> {
    boolean existsByAgendamentoIdAndTipoAndSucessoTrue(UUID agendamentoId, TipoLembrete tipo);
    boolean existsByIdExterno(String idExterno);
    boolean existsByChaveDestinoAndTextoAndSucessoTrueAndEnviadoEmAfter(String chaveDestino, String texto, Instant desde);
    List<RegistroMensagemEntity> findByAgendamentoIdOrderByEnviadoEmAscIdAsc(UUID agendamentoId);
    Optional<RegistroMensagemEntity> findFirstByChaveDestinoAndTipoAndSucessoTrueOrderByEnviadoEmDescIdDesc(String chaveDestino, TipoLembrete tipo);

    @Modifying
    @Query(value = "update registros_mensagem set entregue_em = :em where id_externo = :idExterno and entregue_em is null", nativeQuery = true)
    int marcarEntregue(@Param("idExterno") String idExterno, @Param("em") Instant em);
}
