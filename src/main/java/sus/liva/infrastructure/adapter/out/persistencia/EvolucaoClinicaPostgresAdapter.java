package sus.liva.infrastructure.adapter.out.persistencia;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sus.liva.application.port.out.EvolucaoClinicaPort;
import sus.liva.domain.model.agendamento.EvolucaoClinica;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class EvolucaoClinicaPostgresAdapter implements EvolucaoClinicaPort {
    private final EvolucaoClinicaJpaRepository repository;

    EvolucaoClinicaPostgresAdapter(EvolucaoClinicaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void salvarEvolucao(EvolucaoClinica evolucao) {
        repository.save(EvolucaoClinicaEntity.de(evolucao));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvolucaoClinica> evolucoesDas(Collection<UUID> agendamentoIds) {
        if (agendamentoIds.isEmpty()) return List.of();
        return repository.findByAgendamentoIdInOrderByRegistradaEm(agendamentoIds).stream()
                .map(EvolucaoClinicaEntity::paraDominio).toList();
    }
}
