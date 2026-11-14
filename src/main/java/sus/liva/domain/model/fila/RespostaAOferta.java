package sus.liva.domain.model.fila;

import java.text.Normalizer;
import java.util.Optional;
import java.util.Set;

public enum RespostaAOferta {
    ACEITAR(Set.of("aceito", "aceitar", "aceito a vaga", "quero a vaga", "aceito sim", "sim aceito")),
    RECUSAR(Set.of("nao posso", "nao posso neste horario", "nao posso nesse horario", "recuso", "recusar", "nao quero a vaga"));

    private final Set<String> palavras;

    RespostaAOferta(Set<String> palavras) {
        this.palavras = palavras;
    }

    public static Optional<RespostaAOferta> interpretar(String texto) {
        if (texto == null) return Optional.empty();
        String normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .trim()
                .replaceAll("\\s+", " ");
        for (RespostaAOferta r : values()) if (r.palavras.contains(normalizado)) return Optional.of(r);
        return Optional.empty();
    }
}
