package sus.liva.application.port.out;

import java.time.Instant;

public record CodigoDoCuidador(String codigoHash, Instant expiraEm, Instant enviadoEm, int tentativas) {}
