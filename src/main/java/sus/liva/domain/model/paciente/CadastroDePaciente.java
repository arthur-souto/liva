package sus.liva.domain.model.paciente;

import java.time.Instant;
import java.util.UUID;

public record CadastroDePaciente(
        Paciente paciente,
        long versao,
        Instant criadoEm,
        UUID criadoPor,
        Instant atualizadoEm
) {}
