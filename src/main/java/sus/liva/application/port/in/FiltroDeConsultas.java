package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.StatusAgendamento;

public record FiltroDeConsultas(PeriodoDasConsultas periodo, String especialidade, StatusAgendamento situacao, Integer ano, String busca) {}
