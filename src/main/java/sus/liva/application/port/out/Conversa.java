package sus.liva.application.port.out;

import sus.liva.domain.model.Telefone;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record Conversa(Telefone telefone, UUID pacienteId, EtapaDaConversa etapa, List<String> opcoes, Instant atualizadaEm) {
    public Conversa {
        opcoes = opcoes == null ? List.of() : List.copyOf(opcoes);
    }
}
