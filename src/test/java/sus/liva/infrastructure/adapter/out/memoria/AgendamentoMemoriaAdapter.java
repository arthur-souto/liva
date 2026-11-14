package sus.liva.infrastructure.adapter.out.memoria;

import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.domain.model.agendamento.Agendamento;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
                .sorted(java.util.Comparator.comparing(Agendamento::getDataHora))
                .toList();
    }

    @Override
    public void salvarAgendamento(Agendamento agendamento) {
        dados.put(agendamento.getId(), agendamento);
    }
}
