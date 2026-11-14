package sus.liva.application.usecase;

import sus.liva.application.exception.NaoEncontradoException;
import sus.liva.application.port.in.ConsultarHistoricoUseCase;
import sus.liva.application.port.in.MensagemDoHistorico;
import sus.liva.application.port.out.AgendamentoRepositoryPort;
import sus.liva.application.port.out.MensagemRecebidaPort;
import sus.liva.application.port.out.RegistroMensagemPort;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class ConsultarHistoricoUseCaseImpl implements ConsultarHistoricoUseCase {
    private final AgendamentoRepositoryPort agendamentos;
    private final RegistroMensagemPort registros;
    private final MensagemRecebidaPort recebidas;

    public ConsultarHistoricoUseCaseImpl(AgendamentoRepositoryPort agendamentos, RegistroMensagemPort registros,
                                         MensagemRecebidaPort recebidas) {
        this.agendamentos = agendamentos;
        this.registros = registros;
        this.recebidas = recebidas;
    }

    @Override
    public List<MensagemDoHistorico> historico(UUID agendamentoId) {
        if (agendamentos.buscarAgendamento(agendamentoId).isEmpty())
            throw new NaoEncontradoException("Agendamento não encontrado: " + agendamentoId);
        return Stream.concat(
                        registros.enviadasDoAgendamento(agendamentoId).stream().map(MensagemDoHistorico::de),
                        recebidas.recebidasDoAgendamento(agendamentoId).stream().map(MensagemDoHistorico::de))
                .sorted(Comparator.comparing(MensagemDoHistorico::em))
                .toList();
    }
}
