package sus.liva.application.port.in;

import java.time.Instant;
import java.util.List;

public record MinhaFila(Instant agora, List<PosicaoNaFila> filas, List<OfertaParaPaciente> ofertas) {}
