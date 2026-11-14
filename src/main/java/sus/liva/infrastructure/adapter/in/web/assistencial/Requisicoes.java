package sus.liva.infrastructure.adapter.in.web.assistencial;

import sus.liva.domain.model.triagem.AvaliacaoClinica;
import sus.liva.domain.model.triagem.Prioridade;
import sus.liva.domain.model.triagem.SinaisVitais;

import java.time.LocalDateTime;
import java.util.UUID;

public final class Requisicoes {
    private Requisicoes() {
    }

    public record Chegada(UUID pacienteId) {}

    public record Avaliacao(AvaliacaoClinica avaliacao, SinaisVitais sinaisVitais, Prioridade prioridade,
                            String justificativaPrioridade, Long versao) {}

    public record ComVersao(Long versao) {}

    public record Motivo(String motivo) {}

    public record NovaRetificacao(String texto, String justificativa) {}

    public record Desfecho(String desfecho) {}

    public record NovaConsulta(UUID profissionalId, LocalDateTime inicio) {}

    public record Remarcacao(UUID profissionalId, LocalDateTime inicio, Long versao) {}

    public record Finalizacao(String evolucao, boolean autorizarRetorno, Long versao) {}

    public record NovaEvolucao(String texto) {}

    static Boolean andamento(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return switch (valor.trim().toUpperCase()) {
            case "ABERTO" -> true;
            case "ENCERRADO" -> false;
            default -> throw new IllegalArgumentException("Use andamento=ABERTO ou andamento=ENCERRADO");
        };
    }
}
