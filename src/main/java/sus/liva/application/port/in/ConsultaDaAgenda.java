package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.encaminhamento.Encaminhamento;

public record ConsultaDaAgenda(Agendamento consulta, String nomePaciente, Encaminhamento encaminhamento) {}
