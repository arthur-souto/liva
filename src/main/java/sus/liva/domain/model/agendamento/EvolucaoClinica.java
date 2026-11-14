package sus.liva.domain.model.agendamento;

import java.time.Instant;
import java.util.UUID;

public record EvolucaoClinica(
        UUID agendamentoId,
        UUID autorId,
        String texto,
        Instant registradaEm
) {}
