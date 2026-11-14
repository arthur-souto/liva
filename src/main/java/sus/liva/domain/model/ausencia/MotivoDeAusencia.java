package sus.liva.domain.model.ausencia;

import java.text.Normalizer;
import java.util.Optional;
import java.util.Set;

public enum MotivoDeAusencia {
    ESQUECI("1", "Esqueci da consulta", Set.of("esqueci", "esquecimento", "me esqueci", "esqueci da consulta")),
    TRANSPORTE("2", "Transporte ou distância", Set.of("transporte", "onibus", "distancia", "longe", "sem passagem", "sem dinheiro")),
    TRABALHO("3", "Trabalho, estudo ou cuidar de alguém", Set.of("trabalho", "trabalhando", "estudo", "escola", "cuidar", "filho", "filhos")),
    SAUDE("4", "Estava doente ou sem condições no dia", Set.of("doente", "passei mal", "estava doente", "internado", "gripe")),
    NAO_PRECISA("5", "Não preciso mais da consulta", Set.of("nao preciso", "nao preciso mais", "melhorei", "ja melhorei", "resolvi")),
    OUTRO("6", "Outro motivo", Set.of("outro", "outro motivo", "outros"));

    private final String numero;
    private final String rotulo;
    private final Set<String> palavras;

    MotivoDeAusencia(String numero, String rotulo, Set<String> palavras) {
        this.numero = numero;
        this.rotulo = rotulo;
        this.palavras = palavras;
    }

    public String getNumero() {
        return numero;
    }

    public String getRotulo() {
        return rotulo;
    }

    public static Optional<MotivoDeAusencia> interpretar(String texto) {
        if (texto == null) return Optional.empty();

        String normalizado = normalizar(texto);
        if (normalizado.isEmpty()) return Optional.empty();

        String primeiro = normalizado.split(" ")[0];
        for (MotivoDeAusencia m : values())
            if (m.numero.equals(primeiro) || m.palavras.contains(normalizado)) return Optional.of(m);

        for (MotivoDeAusencia m : values())
            for (String palavra : m.palavras)
                if (palavra.contains(" ") && normalizado.contains(palavra)) return Optional.of(m);

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
