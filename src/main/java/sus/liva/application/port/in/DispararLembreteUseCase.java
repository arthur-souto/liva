package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.TipoLembrete;

import java.util.UUID;

public interface DispararLembreteUseCase {
    ResultadoDisparo disparar(UUID agendamentoId, TipoLembrete tipo, boolean forcar);
}
