package sus.liva.application.port.in;

import sus.liva.domain.model.agendamento.Agendamento;

public interface AgendarAtendimentoUseCase {
    Agendamento agendar(NovoAtendimento novo);
}
