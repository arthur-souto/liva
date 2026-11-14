package sus.liva.domain.model.cuidador;

import java.time.Instant;

public record CuidadorVinculado(Cuidador cuidador, Parentesco parentesco, Instant autorizadoEm) {}
