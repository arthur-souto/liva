package sus.liva.infrastructure.adapter.in.web;

import sus.liva.application.port.in.AtendimentoAgendado;
import sus.liva.application.port.in.ResultadoDisparo;

import java.util.UUID;

public record AgendamentoCriadoResponse(
        UUID pacienteId,
        boolean pacienteNovo,
        AgendamentoResponse agendamento,
        ResultadoDisparo confirmacao,
        ResultadoDisparo pedidoConfirmacao
) {
    public static AgendamentoCriadoResponse de(AtendimentoAgendado a) {
        return new AgendamentoCriadoResponse(a.agendamento().getPacienteId(), a.pacienteNovo(),
                AgendamentoResponse.de(a.agendamento()), a.confirmacao(), a.pedidoConfirmacao());
    }
}
