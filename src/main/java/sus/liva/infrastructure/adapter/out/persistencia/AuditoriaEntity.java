package sus.liva.infrastructure.adapter.out.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import sus.liva.application.port.out.EventoAuditoria;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auditoria")
public class AuditoriaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "ator_id", nullable = false, updatable = false)
    private UUID atorId;
    @Column(nullable = false, length = 20, updatable = false)
    private String papel;
    @Column(nullable = false, length = 60, updatable = false)
    private String acao;
    @Column(nullable = false, length = 30, updatable = false)
    private String recurso;
    @Column(name = "recurso_id", nullable = false, length = 64, updatable = false)
    private String recursoId;
    @Column(name = "paciente_id", updatable = false)
    private UUID pacienteId;
    @Column(name = "registrada_em", nullable = false, updatable = false)
    private Instant registradaEm;

    protected AuditoriaEntity() {
    }

    static AuditoriaEntity de(EventoAuditoria e) {
        AuditoriaEntity n = new AuditoriaEntity();
        n.atorId = e.atorId();
        n.papel = e.papel();
        n.acao = e.acao();
        n.recurso = e.recurso();
        n.recursoId = e.recursoId();
        n.pacienteId = e.pacienteId();
        n.registradaEm = e.registradaEm();
        return n;
    }
}
