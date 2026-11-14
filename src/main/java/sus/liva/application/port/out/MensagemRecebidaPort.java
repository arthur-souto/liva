package sus.liva.application.port.out;

import sus.liva.domain.model.mensagem.MensagemRecebida;

import java.util.List;
import java.util.UUID;

public interface MensagemRecebidaPort {
    void salvarRecebida(MensagemRecebida mensagem);
    List<MensagemRecebida> recebidasDoAgendamento(UUID agendamentoId);
}
