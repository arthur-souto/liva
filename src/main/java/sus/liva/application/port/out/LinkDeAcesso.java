package sus.liva.application.port.out;

import java.time.Instant;
import java.util.UUID;

public record LinkDeAcesso(String tokenHash, UUID pacienteId, String destino, Instant criadoEm, Instant expiraEm, Instant usadoEm) {
    public boolean validoEm(Instant agora) {
        return usadoEm == null && expiraEm.isAfter(agora);
    }
}
