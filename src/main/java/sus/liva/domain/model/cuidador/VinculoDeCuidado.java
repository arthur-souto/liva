package sus.liva.domain.model.cuidador;

import java.time.Instant;
import java.util.UUID;

public record VinculoDeCuidado(UUID cuidadorId, UUID pacienteId, Parentesco parentesco, UUID autorizadoPor, Instant autorizadoEm) {}
