package sus.liva.application.port.out;

import java.time.Instant;

public record CredencialDoCuidador(String senhaHash, int tentativasFalhas, Instant bloqueadoAte) {
    public boolean bloqueadaEm(Instant agora) {
        return bloqueadoAte != null && bloqueadoAte.isAfter(agora);
    }
}
