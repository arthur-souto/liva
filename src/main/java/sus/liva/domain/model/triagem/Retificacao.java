package sus.liva.domain.model.triagem;

import java.time.Instant;
import java.util.UUID;

public record Retificacao(UUID triagemId, UUID autorId, String texto, String justificativa, Instant registradaEm) {}
