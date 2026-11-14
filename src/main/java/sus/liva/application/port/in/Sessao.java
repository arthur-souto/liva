package sus.liva.application.port.in;

import java.time.Instant;

public record Sessao(String token, Instant expiraEm, Ator ator, String nome) {}
