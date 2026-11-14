package sus.liva.domain.model.ausencia;

import java.time.Instant;
import java.util.UUID;

public record MotivoRegistrado(
        UUID agendamentoId,
        UUID pacienteId,
        OcorrenciaDeAusencia ocorrencia,
        MotivoDeAusencia motivo,
        CanalDoMotivo canal,
        Instant perguntadoEm,
        Instant respondidoEm
) {}
