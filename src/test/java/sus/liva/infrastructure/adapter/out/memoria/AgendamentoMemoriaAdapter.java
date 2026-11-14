package sus.liva.infrastructure.adapter.out.memoria;

import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.domain.model.agendamento.Agendamento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class AgendamentoMemoriaAdapter implements AgendamentoRepositoryPort {
    private final Map<UUID, Agendamento> dados = new ConcurrentHashMap<>();

    @Override
    public Optional<Agendamento> buscarAgendamento(UUID id) {
        return Optional.ofNullable(dados.get(id));
    }

    @Override
    public Optional<Agendamento> buscarAgendamentoParaAtualizar(UUID id) {
        return buscarAgendamento(id);
    }

    @Override
    public List<Agendamento> agendamentosDoDia(LocalDate data) {
        return dados.values().stream()
                .filter(a -> a.getDataHora().toLocalDate().equals(data))
                .toList();
    }

    @Override
    public List<Agendamento> agendamentosEntre(LocalDate inicio, LocalDate fim) {
        return dados.values().stream()
                .filter(a -> !a.getDataHora().toLocalDate().isBefore(inicio) && !a.getDataHora().toLocalDate().isAfter(fim))
                .sorted(Comparator.comparing(Agendamento::getDataHora))
                .toList();
    }

    @Override
    public void salvarAgendamento(Agendamento agendamento) {
        dados.put(agendamento.getId(), agendamento);
    }

    @Override
    public Optional<Agendamento> buscarPorChaveIdempotencia(String chave) {
        return dados.values().stream().filter(a -> chave.equals(a.getChaveIdempotencia())).findFirst();
    }

    @Override
    public List<Agendamento> consultasDoEncaminhamento(UUID encaminhamentoId) {
        return ordenadas(a -> encaminhamentoId.equals(a.getEncaminhamentoId()));
    }

    @Override
    public List<Agendamento> consultasDoPaciente(UUID pacienteId) {
        return ordenadas(a -> pacienteId.equals(a.getPacienteId()));
    }

    @Override
    public List<Agendamento> consultasDoProfissional(UUID profissionalId, LocalDateTime inicio, LocalDateTime fim) {
        return ordenadas(a -> profissionalId.equals(a.getProfissionalId())
                && !a.getDataHora().isBefore(inicio) && a.getDataHora().isBefore(fim));
    }

    private List<Agendamento> ordenadas(Predicate<Agendamento> filtro) {
        return dados.values().stream().filter(filtro).sorted(Comparator.comparing(Agendamento::getDataHora)).toList();
    }
}
