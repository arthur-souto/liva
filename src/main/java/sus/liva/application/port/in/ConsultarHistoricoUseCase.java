package sus.liva.application.port.in;

import java.util.List;
import java.util.UUID;

public interface ConsultarHistoricoUseCase {
    List<MensagemDoHistorico> historico(UUID agendamentoId);
}
