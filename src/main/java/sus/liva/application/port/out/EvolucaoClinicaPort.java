package sus.liva.application.port.out;

import sus.liva.domain.model.agendamento.EvolucaoClinica;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface EvolucaoClinicaPort {
    void salvarEvolucao(EvolucaoClinica evolucao);
    List<EvolucaoClinica> evolucoesDas(Collection<UUID> agendamentoIds);
}
