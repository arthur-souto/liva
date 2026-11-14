package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.Agendamento;

import java.util.Optional;
import java.util.UUID;

public interface ConsultarAgendamentoUseCase {
    Optional<Agendamento> consultar(UUID id);
}
