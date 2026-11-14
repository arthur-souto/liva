package sus.liva.application.port.out;

import sus.liva.application.port.in.Ator;
import sus.liva.application.port.in.Papel;

import java.time.Instant;
import java.util.UUID;

public record ContaDeAcesso(UUID id, String login, String nome, String senhaHash, Papel papel, UUID profissionalId,
                            UUID pacienteId, boolean ativo, int tentativasFalhas, Instant bloqueadaAte) {

    public boolean bloqueadaEm(Instant agora) {
        return bloqueadaAte != null && bloqueadaAte.isAfter(agora);
    }

    public Ator ator() {
        return new Ator(id, papel, profissionalId, pacienteId);
    }
}
