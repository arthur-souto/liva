package sus.liva.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record CodigoDoPortal(UUID pacienteId, String codigoHash, Instant expiraEm, Instant enviadoEm, int tentativas) {}
