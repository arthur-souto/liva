package sus.liva.application.port.out;

import sus.liva.domain.model.fila.EventoDaFila;

import java.time.Instant;
import java.util.UUID;

public record Movimentacao(
        UUID entradaId,
        UUID ofertaId,
        UUID vagaId,
        UUID agendamentoId,
        UUID pacienteId,
        EventoDaFila evento,
        String detalhe,
        UUID atorId,
        String papel,
        Instant registradaEm
) {}
