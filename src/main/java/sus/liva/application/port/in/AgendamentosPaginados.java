package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.StatusAgendamento;

import java.util.Map;

public record AgendamentosPaginados(Pagina<AgendamentoDetalhado> pagina, long totalDoPeriodo, Map<StatusAgendamento, Long> porStatus) {}
