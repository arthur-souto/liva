package sus.liva.application.usecase;

import sus.liva.application.port.in.AgendamentoDetalhado;
import sus.liva.application.port.in.ListarAgendamentosUseCase;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.PacienteRepositoryPort;
import sus.liva.domain.model.agendamento.StatusAgendamento;
import sus.liva.domain.model.paciente.Paciente;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class ListarAgendamentosUseCaseImpl implements ListarAgendamentosUseCase {
    private static final int MAXIMO_DE_DIAS = 31;

    private final AgendamentoRepositoryPort agendamentos;
    private final PacienteRepositoryPort pacientes;

    public ListarAgendamentosUseCaseImpl(AgendamentoRepositoryPort agendamentos, PacienteRepositoryPort pacientes) {
        this.agendamentos = agendamentos;
        this.pacientes = pacientes;
    }

    @Override
    public List<AgendamentoDetalhado> listar(LocalDate inicio, LocalDate fim, Set<StatusAgendamento> status) {
        if (inicio == null || fim == null) throw new IllegalArgumentException("Informe o período");
        if (fim.isBefore(inicio)) throw new IllegalArgumentException("A data final deve ser igual ou posterior à inicial");
        if (ChronoUnit.DAYS.between(inicio, fim) >= MAXIMO_DE_DIAS)
            throw new IllegalArgumentException("O período pode ter no máximo " + MAXIMO_DE_DIAS + " dias");

        Map<UUID, Optional<Paciente>> cache = new HashMap<>();
        return agendamentos.agendamentosEntre(inicio, fim).stream()
                .filter(a -> status == null || status.isEmpty() || status.contains(a.getStatus()))
                .map(a -> new AgendamentoDetalhado(a,
                        cache.computeIfAbsent(a.getPacienteId(), pacientes::buscarPaciente).orElse(null)))
                .toList();
    }
}
