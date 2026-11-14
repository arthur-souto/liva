package sus.liva.application.usecase;

import sus.liva.application.port.in.ConsultarAgendamentoUseCase;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.domain.model.agendamento.Agendamento;

import java.util.Optional;
import java.util.UUID;

public class ConsultarAgendamentoUseCaseImpl implements ConsultarAgendamentoUseCase {
    private final AgendamentoRepositoryPort agendamentos;

    public ConsultarAgendamentoUseCaseImpl(AgendamentoRepositoryPort agendamentos) {
        this.agendamentos = agendamentos;
    }

    @Override
    public Optional<Agendamento> consultar(UUID id) {
        return agendamentos.buscarAgendamento(id);
    }
}
