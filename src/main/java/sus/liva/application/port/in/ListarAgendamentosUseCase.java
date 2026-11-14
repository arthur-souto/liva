package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.StatusAgendamento;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface ListarAgendamentosUseCase {
    List<AgendamentoDetalhado> listar(LocalDate inicio, LocalDate fim, Set<StatusAgendamento> status);
    AgendamentosPaginados listar(LocalDate inicio, LocalDate fim, Set<StatusAgendamento> status, String busca, PedidoDePagina pedido);
}
