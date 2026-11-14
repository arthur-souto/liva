package sus.liva.application.port.in;

import java.util.UUID;

public record FiltroDaFila(String especialidade, UUID profissionalId, String unidade) {
    public static FiltroDaFila todas() {
        return new FiltroDaFila(null, null, null);
    }
}
