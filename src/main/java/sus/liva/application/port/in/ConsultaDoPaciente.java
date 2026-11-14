package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.Agendamento;

public record ConsultaDoPaciente(Agendamento consulta, ResumoDoEncaminhamento encaminhamento, String especialidade, String profissional) {}
