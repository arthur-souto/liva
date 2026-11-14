package sus.liva.application.usecase;

import sus.liva.application.port.in.DispararLembreteUseCase;
import sus.liva.application.port.in.DispararLembretesDoDiaUseCase;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.domain.model.agendamento.Agendamento;
import sus.liva.domain.model.agendamento.TipoLembrete;

import java.time.LocalDate;
import java.util.List;

public class DispararLembretesDoDiaUseCaseImpl implements DispararLembretesDoDiaUseCase {
    private final AgendamentoRepositoryPort agendamentos;
    private final DispararLembreteUseCase dispararLembrete;

    public DispararLembretesDoDiaUseCaseImpl(AgendamentoRepositoryPort agendamentos,
                                             DispararLembreteUseCase dispararLembrete) {
        this.agendamentos = agendamentos;
        this.dispararLembrete = dispararLembrete;
    }

    @Override
    public int dispararPendentes(LocalDate hoje) {
        int total = 0;
        for (TipoLembrete tipo : List.of(TipoLembrete.PEDIDO_CONFIRMACAO, TipoLembrete.LEMBRETE_VESPERA))
            for (Agendamento a : agendamentos.agendamentosDoDia(hoje.plusDays(tipo.getDiasAntes())))
                total += dispararLembrete.disparar(a.getId(), tipo, false).envios().size();
        return total;
    }
}
