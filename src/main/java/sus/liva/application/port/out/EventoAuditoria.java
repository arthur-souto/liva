package sus.liva.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record EventoAuditoria(UUID atorId, String papel, String acao, String recurso, String recursoId, UUID pacienteId,
                              Instant registradaEm) {}
