package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.Agendamento;

public record AtendimentoAgendado(
        Agendamento agendamento,
        boolean pacienteNovo,
        ResultadoDisparo confirmacao,
        ResultadoDisparo pedidoConfirmacao
) {}
