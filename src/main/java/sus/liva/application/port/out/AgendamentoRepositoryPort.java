package sus.liva.application.port.out;

import sus.liva.domain.model.agendamento.Agendamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgendamentoRepositoryPort {
    Optional<Agendamento> buscarAgendamento(UUID id);
    Optional<Agendamento> buscarAgendamentoParaAtualizar(UUID id);
    List<Agendamento> agendamentosDoDia(LocalDate data);
    List<Agendamento> agendamentosEntre(LocalDate inicio, LocalDate fim);
    void salvarAgendamento(Agendamento agendamento);
    Optional<Agendamento> buscarPorChaveIdempotencia(String chave);
    List<Agendamento> consultasDoEncaminhamento(UUID encaminhamentoId);
    List<Agendamento> consultasDoPaciente(UUID pacienteId);
    List<Agendamento> consultasDoProfissional(UUID profissionalId, LocalDateTime inicio, LocalDateTime fim);
}
