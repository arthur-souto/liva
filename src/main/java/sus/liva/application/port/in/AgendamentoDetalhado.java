package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.paciente.Paciente;

public record AgendamentoDetalhado(Agendamento agendamento, Paciente paciente) {}
