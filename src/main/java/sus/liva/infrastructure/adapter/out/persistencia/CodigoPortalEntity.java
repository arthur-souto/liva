package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sus.liva.application.port.out.CodigoDoPortal;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "codigos_portal")
public class CodigoPortalEntity {
    @Id
    @Column(name = "paciente_id")
    private UUID pacienteId;
    @Column(name = "codigo_hash", nullable = false, length = 100)
    private String codigoHash;
    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;
    @Column(name = "enviado_em", nullable = false)
    private Instant enviadoEm;
    @Column(nullable = false)
    private int tentativas;

    protected CodigoPortalEntity() {
    }

    CodigoDoPortal paraDominio() {
        return new CodigoDoPortal(pacienteId, codigoHash, expiraEm, enviadoEm, tentativas);
    }
}
