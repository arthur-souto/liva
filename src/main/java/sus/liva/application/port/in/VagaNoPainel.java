package sus.liva.application.port.in;

import sus.liva.domain.model.fila.OrigemDaVaga;
import sus.liva.domain.model.fila.StatusVaga;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record VagaNoPainel(
        UUID id,
        String nomeEspecialidade,
        String profissional,
        String unidade,
        LocalDateTime inicio,
        int duracaoMinutos,
        OrigemDaVaga origem,
        StatusVaga status,
        String motivo,
        int ofertas,
        Instant liberadaEm
) {}
