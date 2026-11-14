package sus.liva.application.port.in;

import sus.liva.domain.model.fila.CanalDeResposta;
import sus.liva.domain.model.fila.StatusOferta;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record OfertaNoPainel(
        UUID id,
        UUID entradaId,
        UUID pacienteId,
        String paciente,
        String nomeEspecialidade,
        String profissional,
        LocalDateTime inicio,
        StatusOferta status,
        Instant criadaEm,
        Instant expiraEm,
        Instant enviadaEm,
        Instant respondidaEm,
        CanalDeResposta canal,
        String motivo
) {}
