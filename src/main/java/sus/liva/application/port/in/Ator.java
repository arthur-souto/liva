package sus.liva.application.port.in;

import java.util.UUID;

public record Ator(UUID usuarioId, Papel papel, UUID profissionalId, UUID pacienteId) {
    public static Ator profissional(UUID usuarioId, Papel papel, UUID profissionalId) {
        return new Ator(usuarioId, papel, profissionalId, null);
    }

    public static Ator paciente(UUID usuarioId, UUID pacienteId) {
        return new Ator(usuarioId, Papel.PACIENTE, null, pacienteId);
    }

    public static Ator equipe(UUID usuarioId, Papel papel) {
        return new Ator(usuarioId, papel, null, null);
    }

    public boolean eh(Papel... papeis) {
        for (Papel p : papeis) if (p == papel) return true;
        return false;
    }
}
