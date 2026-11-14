package sus.liva.infrastructure.adapter.in.web;

import sus.liva.application.port.in.AgendamentoDetalhado;
import sus.liva.application.port.in.AgendamentosPaginados;
import sus.liva.application.port.in.Pagina;
import sus.liva.domain.model.agendamento.StatusAgendamento;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AgendamentosDaEquipeResponse(
        LocalDate inicio,
        LocalDate fim,
        long total,
        long encontrados,
        int pagina,
        int tamanho,
        int totalPaginas,
        Map<StatusAgendamento, Long> porStatus,
        List<AgendamentoDaEquipeResponse> agendamentos
) {
    public static AgendamentosDaEquipeResponse de(LocalDate inicio, LocalDate fim, List<AgendamentoDetalhado> lista) {
        Map<StatusAgendamento, Long> porStatus = new LinkedHashMap<>();
        Arrays.stream(StatusAgendamento.values()).forEach(s -> porStatus.put(s,
                lista.stream().filter(d -> d.agendamento().getStatus() == s).count()));
        return new AgendamentosDaEquipeResponse(inicio, fim, lista.size(), lista.size(), 1, Math.max(1, lista.size()), 1, porStatus,
                lista.stream().map(AgendamentoDaEquipeResponse::de).toList());
    }

    public static AgendamentosDaEquipeResponse de(LocalDate inicio, LocalDate fim, AgendamentosPaginados paginados) {
        Pagina<AgendamentoDetalhado> p = paginados.pagina();
        return new AgendamentosDaEquipeResponse(inicio, fim, paginados.totalDoPeriodo(), p.total(), p.numero(), p.tamanho(),
                p.totalPaginas(), paginados.porStatus(), p.itens().stream().map(AgendamentoDaEquipeResponse::de).toList());
    }
}
