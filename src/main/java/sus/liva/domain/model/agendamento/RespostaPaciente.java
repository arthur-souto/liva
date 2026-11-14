package sus.liva.domain.model.agendamento;

import java.text.Normalizer;
import java.util.Optional;
import java.util.Set;

public enum RespostaPaciente {
    CONFIRMAR(StatusAgendamento.CONFIRMADO, "1", Set.of("sim", "s", "vou", "sim vou", "confirmo", "confirmar", "confirmado")),
    CANCELAR(StatusAgendamento.CANCELADO, "2", Set.of("nao", "n", "nao vou", "nao vou conseguir", "cancelar", "cancela", "cancelo")),
    REAGENDAR(StatusAgendamento.REAGENDADO, "3", Set.of("outro horario", "quero outro horario", "remarcar", "reagendar"));

    private final StatusAgendamento statusResultante;
    private final String numero;
    private final Set<String> palavras;

    RespostaPaciente(StatusAgendamento statusResultante, String numero, Set<String> palavras) {
        this.statusResultante = statusResultante;
        this.numero = numero;
        this.palavras = palavras;
    }

    public StatusAgendamento getStatusResultante() {
        return statusResultante;
    }

    public static Optional<RespostaPaciente> interpretar(String texto) {
        if (texto == null) return Optional.empty();

        String normalizado = normalizar(texto);
        if (normalizado.isEmpty()) return Optional.empty();

        String primeiro = normalizado.split(" ")[0];
        for (RespostaPaciente r : values())
            if (r.numero.equals(primeiro) || r.palavras.contains(normalizado)) return Optional.of(r);
        return Optional.empty();
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }
}
