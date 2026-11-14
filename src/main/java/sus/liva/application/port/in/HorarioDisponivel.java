package sus.liva.application.port.in;

import java.time.LocalDateTime;
import java.util.UUID;

public record HorarioDisponivel(UUID profissionalId, String nomeProfissional, LocalDateTime inicio, LocalDateTime fim) {}
