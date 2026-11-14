package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.domain.model.agendamento.Agendamento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AgendamentoPostgresAdapter implements AgendamentoRepositoryPort {
    private final AgendamentoJpaRepository repository;

    AgendamentoPostgresAdapter(AgendamentoJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Agendamento> buscarAgendamento(UUID id) {
        return repository.findById(id).map(AgendamentoEntity::paraDominio);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Agendamento> buscarAgendamentoParaAtualizar(UUID id) {
        return repository.buscarComTrava(id).map(AgendamentoEntity::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agendamento> agendamentosDoDia(LocalDate data) {
        return agendamentosEntre(data, data);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Agendamento> agendamentosEntre(LocalDate inicio, LocalDate fim) {
        return repository.findByDataHoraGreaterThanEqualAndDataHoraLessThanOrderByDataHora(
                        inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay())
                .stream()
                .map(AgendamentoEntity::paraDominio)
                .toList();
    }

    @Override
    @Transactional
    public void salvarAgendamento(Agendamento agendamento) {
        repository.save(AgendamentoEntity.de(agendamento));
    }
}
