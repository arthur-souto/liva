package sus.liva.application.port.out;

import java.time.Instant;

public record TokenEmitido(String token, Instant expiraEm) {}
