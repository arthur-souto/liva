package sus.liva.domain.model.ausencia;

import java.time.Instant;
import java.util.UUID;

public record LigacaoRegistrada(UUID id, UUID agendamentoId, ResultadoDaLigacao resultado, UUID usuarioId, Instant registradaEm) {}
