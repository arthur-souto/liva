package sus.liva.infrastructure.adapter.in.web;

import sus.liva.application.port.in.AgendamentoDetalhado;
import sus.liva.domain.model.agendamento.StatusAgendamento;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AgendamentosDaEquipeResponse(
        LocalDate inicio,
        LocalDate fim,
        int total,
        Map<StatusAgendamento, Long> porStatus,
        List<AgendamentoDaEquipeResponse> agendamentos
) {
    public static AgendamentosDaEquipeResponse de(LocalDate inicio, LocalDate fim, List<AgendamentoDetalhado> lista) {
        Map<StatusAgendamento, Long> porStatus = new LinkedHashMap<>();
        Arrays.stream(StatusAgendamento.values()).forEach(s -> porStatus.put(s,
                lista.stream().filter(d -> d.agendamento().getStatus() == s).count()));
        return new AgendamentosDaEquipeResponse(inicio, fim, lista.size(), porStatus,
                lista.stream().map(AgendamentoDaEquipeResponse::de).toList());
    }
}
