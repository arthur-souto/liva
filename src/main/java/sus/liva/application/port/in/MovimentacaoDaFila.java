package sus.liva.application.port.in;

import sus.liva.domain.model.fila.EventoDaFila;

import java.time.Instant;
import java.util.UUID;

public record MovimentacaoDaFila(
        long id,
        EventoDaFila evento,
        String detalhe,
        UUID entradaId,
        UUID ofertaId,
        UUID vagaId,
        UUID agendamentoId,
        UUID pacienteId,
        String paciente,
        String papel,
        Instant registradaEm
) {}
