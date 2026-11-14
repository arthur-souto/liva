package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

interface CodigoPortalJpaRepository extends JpaRepository<CodigoPortalEntity, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CodigoPortalEntity c where c.pacienteId = :pacienteId")
    Optional<CodigoPortalEntity> buscarComTrava(@Param("pacienteId") UUID pacienteId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            insert into codigos_portal (paciente_id, codigo_hash, expira_em, enviado_em, tentativas)
            values (:pacienteId, :codigoHash, :expiraEm, :enviadoEm, :tentativas)
            on conflict (paciente_id) do update
               set codigo_hash = excluded.codigo_hash, expira_em = excluded.expira_em,
                   enviado_em = excluded.enviado_em, tentativas = excluded.tentativas""", nativeQuery = true)
    void gravar(@Param("pacienteId") UUID pacienteId, @Param("codigoHash") String codigoHash, @Param("expiraEm") Instant expiraEm,
                @Param("enviadoEm") Instant enviadoEm, @Param("tentativas") int tentativas);

    @Modifying(clearAutomatically = true)
    @Query(value = "delete from codigos_portal where paciente_id = :pacienteId", nativeQuery = true)
    void remover(@Param("pacienteId") UUID pacienteId);
}
