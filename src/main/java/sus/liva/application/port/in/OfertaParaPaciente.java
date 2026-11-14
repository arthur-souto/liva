package sus.liva.application.port.in;

import sus.liva.domain.model.fila.StatusOferta;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record OfertaParaPaciente(
        UUID id,
        UUID entradaId,
        String nomeEspecialidade,
        String profissional,
        String unidade,
        String endereco,
        LocalDateTime inicio,
        LocalDateTime fim,
        Instant expiraEm,
        LocalDateTime antecipaDe,
        StatusOferta status
) {}
