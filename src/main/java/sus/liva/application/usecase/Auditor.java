package sus.liva.application.usecase;

import sus.liva.application.port.in.Ator;
import sus.liva.application.port.out.AuditoriaPort;
import sus.liva.application.port.out.EventoAuditoria;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

final class Auditor {
    private final AuditoriaPort auditoria;
    private final Clock clock;

    Auditor(AuditoriaPort auditoria, Clock clock) {
        this.auditoria = auditoria;
        this.clock = clock;
    }

    void registrar(Ator ator, String acao, String recurso, UUID recursoId, UUID pacienteId) {
        auditoria.registrar(new EventoAuditoria(ator.usuarioId(), ator.papel().name(), acao, recurso,
                recursoId.toString(), pacienteId, Instant.now(clock)));
    }
}
